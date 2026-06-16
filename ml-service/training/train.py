"""Train, calibrate, evaluate and persist the win-probability model.

Synthetic (pipeline check):   python -m training.train
Real crawled corpus:          python -m training.train --source postgres
"""
from __future__ import annotations

import argparse
import pickle
import sys

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


def fit_and_evaluate(champion_ids, rows, y, n_estimators: int | None = None):
    """Core training: featurize, calibrate, evaluate. Shared by synthetic and real paths.

    Calibration folds adapt to the corpus size (and are skipped for tiny datasets) so a small
    early crawl still trains without scikit-learn raising on too-few samples per class.
    """
    y = np.asarray(y)
    features = FeatureBuilder(champion_ids)
    x = features.transform(rows)

    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=0)

    base = _base_estimator()
    if n_estimators is not None and hasattr(base, "n_estimators"):
        base.set_params(n_estimators=n_estimators)

    minority = int(min(np.bincount(y_train))) if len(np.unique(y_train)) > 1 else 0
    if minority >= 2:
        cv = min(3, minority)
        classifier = CalibratedClassifierCV(base, method="isotonic", cv=cv)
        calibrated = True
    else:
        classifier = base  # too few examples to calibrate; fit the base estimator directly
        calibrated = False
    classifier.fit(x_train, y_train)

    probabilities = classifier.predict_proba(x_test)[:, 1]
    metrics = {
        "accuracy": round(float(accuracy_score(y_test, probabilities > 0.5)), 4),
        "log_loss": round(float(log_loss(y_test, probabilities, labels=[0, 1])), 4),
        "ece": round(float(expected_calibration_error(np.asarray(y_test), probabilities)), 4),
        "n_train": int(len(x_train)),
        "n_test": int(len(x_test)),
        "calibrated": calibrated,
    }
    bundle = {"model": classifier, "champion_ids": champion_ids, "metrics": metrics}
    return bundle, metrics


def train_model(n_matches: int = 8000, n_estimators: int | None = None, seed: int = 42):
    """Train on synthetic drafts (pipeline check)."""
    champion_ids, rows, y = generate(n_matches=n_matches, seed=seed)
    return fit_and_evaluate(champion_ids, rows, y, n_estimators=n_estimators)


def train_from_postgres(dsn: str | None = None, n_estimators: int | None = None, min_matches: int = 200):
    """Train on the real crawled corpus from Postgres."""
    from training.load_real import load_from_postgres

    champion_ids, rows, y = load_from_postgres(dsn)
    if len(rows) < min_matches:
        raise SystemExit(
            f"Only {len(rows)} usable matches in the corpus (need >= {min_matches}). "
            "Run a larger crawl first (POST /api/v1/admin/crawl)."
        )
    return fit_and_evaluate(champion_ids, rows, y, n_estimators=n_estimators)


def main() -> None:
    parser = argparse.ArgumentParser(description="Train the win-probability model.")
    parser.add_argument("--source", choices=["synthetic", "postgres"], default="synthetic")
    parser.add_argument("--dsn", default=None, help="Postgres DSN (else LEAGUETOOL_DB_URL / POSTGRES_*)")
    parser.add_argument("--min-matches", type=int, default=200, help="minimum corpus size for --source postgres")
    args = parser.parse_args()

    if args.source == "postgres":
        bundle, metrics = train_from_postgres(dsn=args.dsn, min_matches=args.min_matches)
        note = (
            "Trained on the REAL crawled corpus. Comp-only win prediction realistically tops out "
            "around 51-55% accuracy; treat the probability as a soft edge, not a verdict."
        )
    else:
        bundle, metrics = train_model()
        note = (
            "Trained on SYNTHETIC drafts as a pipeline check. Run --source postgres after the "
            "Match-V5 crawler has built a corpus for a model that reflects real play."
        )

    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    with open(MODEL_PATH, "wb") as fh:
        pickle.dump(bundle, fh)
    print(f"Saved model to {MODEL_PATH} (source: {args.source})")
    print("Metrics:")
    for key, value in metrics.items():
        print(f"  {key}: {value}")
    print("\n" + note, file=sys.stderr)


if __name__ == "__main__":
    main()
