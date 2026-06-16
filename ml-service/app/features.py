"""Featurization for the win-probability model.

A draft is encoded as a multi-hot vector over the champion universe for the ally team
concatenated with a multi-hot vector for the enemy team. This lets a gradient-boosted model
learn per-champion main effects and (via tree splits) some pairwise interactions, while keeping
the representation independent of any external champion list.
"""
from __future__ import annotations

from collections.abc import Iterable, Sequence

import numpy as np


class FeatureBuilder:
    def __init__(self, champion_ids: Iterable[int]) -> None:
        self.champion_ids: list[int] = sorted(set(int(c) for c in champion_ids))
        self.index: dict[int, int] = {cid: i for i, cid in enumerate(self.champion_ids)}
        self.n = len(self.champion_ids)

    @property
    def n_features(self) -> int:
        return 2 * self.n

    def transform_one(self, allies: Sequence[int], enemies: Sequence[int]) -> np.ndarray:
        vec = np.zeros(self.n_features, dtype=np.float32)
        for champ in allies:
            idx = self.index.get(int(champ))
            if idx is not None:
                vec[idx] = 1.0
        for champ in enemies:
            idx = self.index.get(int(champ))
            if idx is not None:
                vec[self.n + idx] = 1.0
        return vec

    def transform(self, rows: Iterable[tuple[Sequence[int], Sequence[int]]]) -> np.ndarray:
        return np.vstack([self.transform_one(allies, enemies) for allies, enemies in rows])
