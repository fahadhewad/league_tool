"""Train, calibrate, evaluate and persist the win-probability model.

Run with ``python -m training.train`` from the ml-service directory.
"""
from __future__ import annotations

import pickle

import numpy as np
from sklearn.calibration import CalibratedClassifierCV
from sklearn.metrics import accuracy_score, log_loss
from sklearn.model_selection import train_test_split

from app.config import MODEL_PATH, MODELS_DIR
from app.features import FeatureBuilder
from training.synthesize import generate

try:
    from lightgbm import LGBMClassifier

    def _base_estimator():
        return LGBMClassifier(
            n_estimators=300, learning_rate=0.05, num_leaves=31,
            subsample=0.8, colsample_bytree=0.8, verbose=-1,
        )
except ImportError:  # pragma: no cover - fallback if LightGBM's native lib is unavailable
    from sklearn.ensemble import HistGradientBoostingClassifier

    def _base_estimator():
        return HistGradientBoostingClassifier(max_iter=300, learning_rate=0.05)


def expected_calibration_error(y_true: np.ndarray, y_prob: np.ndarray, n_bins: int = 10) -> float:
    bins = np.linspace(0.0, 1.0, n_bins + 1)
    idx = np.digitize(y_prob, bins[1:-1])
    ece = 0.0
    total = len(y_true)
    for b in range(n_bins):
        mask = idx == b
        if not mask.any():
            continue
        confidence = float(y_prob[mask].mean())
        accuracy = float(y_true[mask].mean())
        ece += (mask.sum() / total) * abs(accuracy - confidence)
    return ece


def train_model(n_matches: int = 8000, n_estimators: int | None = None, seed: int = 42):
    champion_ids, rows, y = generate(n_matches=n_matches, seed=seed)
    features = FeatureBuilder(champion_ids)
    x = features.transform(rows)

    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=0)

    base = _base_estimator()
    if n_estimators is not None and hasattr(base, "n_estimators"):
        base.set_params(n_estimators=n_estimators)

    classifier = CalibratedClassifierCV(base, method="isotonic", cv=3)
    classifier.fit(x_train, y_train)

    probabilities = classifier.predict_proba(x_test)[:, 1]
    metrics = {
        "accuracy": round(float(accuracy_score(y_test, probabilities > 0.5)), 4),
        "log_loss": round(float(log_loss(y_test, probabilities)), 4),
        "ece": round(float(expected_calibration_error(np.asarray(y_test), probabilities)), 4),
        "n_train": int(len(x_train)),
        "n_test": int(len(x_test)),
    }
    bundle = {"model": classifier, "champion_ids": champion_ids, "metrics": metrics}
    return bundle, metrics


def main() -> None:
    bundle, metrics = train_model()
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    with open(MODEL_PATH, "wb") as fh:
        pickle.dump(bundle, fh)
    print(f"Saved model to {MODEL_PATH}")
    print("Metrics:")
    for key, value in metrics.items():
        print(f"  {key}: {value}")
    print(
        "\nNote: trained on SYNTHETIC drafts as a pipeline check. Real accuracy comes once the "
        "Match-V5 crawler has built a corpus (comp-only tops out ~51-55%, ~60% with mastery)."
    )


if __name__ == "__main__":
    main()
