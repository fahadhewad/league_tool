import numpy as np

from app.model import WinProbabilityModel
from training.train import expected_calibration_error, train_model


def test_pipeline_learns_the_synthetic_signal():
    bundle, metrics = train_model(n_matches=1500, n_estimators=120, seed=1)

    # The synthetic data has real structure, so the model should beat a coin flip clearly.
    assert metrics["accuracy"] > 0.53
    assert 0.0 <= metrics["ece"] <= 1.0

    model = WinProbabilityModel(bundle["model"], bundle["champion_ids"], metrics)
    champ_ids = bundle["champion_ids"]
    probability = model.predict(champ_ids[:5], champ_ids[5:10])
    assert 0.0 <= probability <= 1.0


def test_expected_calibration_error_is_zero_for_perfect_predictions():
    y_true = np.array([0, 0, 1, 1])
    y_prob = np.array([0.0, 0.0, 1.0, 1.0])
    assert expected_calibration_error(y_true, y_prob) == 0.0
