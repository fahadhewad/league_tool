"""Loading and inference for the trained win-probability model."""
from __future__ import annotations

import pickle
from collections.abc import Sequence
from pathlib import Path

from app.config import MODEL_PATH
from app.features import FeatureBuilder


class WinProbabilityModel:
    def __init__(self, classifier, champion_ids: Sequence[int], metrics: dict | None = None) -> None:
        self.classifier = classifier
        self.features = FeatureBuilder(champion_ids)
        self.metrics = metrics or {}

    @classmethod
    def load(cls, path: Path = MODEL_PATH) -> "WinProbabilityModel":
        with open(path, "rb") as fh:
            bundle = pickle.load(fh)
        return cls(bundle["model"], bundle["champion_ids"], bundle.get("metrics", {}))

    def predict(self, allies: Sequence[int], enemies: Sequence[int]) -> float:
        x = self.features.transform_one(allies, enemies).reshape(1, -1)
        return float(self.classifier.predict_proba(x)[0, 1])

    @property
    def known_champions(self) -> int:
        return self.features.n
