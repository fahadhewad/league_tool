"""Synthetic match generator.

Until the Match-V5 crawler has built a real corpus, we train and exercise the pipeline on
synthetic drafts with a known latent structure: each champion has a hidden strength, plus a
symmetric synergy matrix and an antisymmetric counter matrix. A match outcome is drawn from a
sigmoid of (ally strength + synergy + counter advantage) - (enemy strength + synergy). This gives
a learnable signal with a realistic accuracy ceiling, so metrics on it are meaningful as a
pipeline check rather than a claim about live play.
"""
from __future__ import annotations

import numpy as np

from app.config import TEAM_SIZE


def make_champion_universe(n_champions: int = 60, seed: int = 7):
    rng = np.random.default_rng(seed)
    ids = list(range(1, n_champions + 1))
    strength = rng.normal(0.0, 0.5, size=n_champions)

    synergy = rng.normal(0.0, 0.15, size=(n_champions, n_champions))
    synergy = (synergy + synergy.T) / 2.0
    np.fill_diagonal(synergy, 0.0)

    counter = rng.normal(0.0, 0.2, size=(n_champions, n_champions))
    counter = (counter - counter.T) / 2.0  # antisymmetric

    return ids, strength, synergy, counter


def _team_score(team_idx: np.ndarray, strength: np.ndarray, synergy: np.ndarray) -> float:
    score = float(strength[team_idx].sum())
    for i in range(len(team_idx)):
        for j in range(i + 1, len(team_idx)):
            score += synergy[team_idx[i], team_idx[j]]
    return score


def _counter_advantage(ally_idx: np.ndarray, enemy_idx: np.ndarray, counter: np.ndarray) -> float:
    return float(counter[np.ix_(ally_idx, enemy_idx)].sum())


def generate(n_matches: int = 8000, team_size: int = TEAM_SIZE, seed: int = 42):
    ids, strength, synergy, counter = make_champion_universe()
    n = len(ids)
    rng = np.random.default_rng(seed)

    rows: list[tuple[list[int], list[int]]] = []
    labels = np.zeros(n_matches, dtype=np.int8)

    for m in range(n_matches):
        picks = rng.choice(n, size=2 * team_size, replace=False)
        ally_idx, enemy_idx = picks[:team_size], picks[team_size:]
        diff = (
            _team_score(ally_idx, strength, synergy)
            - _team_score(enemy_idx, strength, synergy)
            + _counter_advantage(ally_idx, enemy_idx, counter)
        )
        prob = 1.0 / (1.0 + np.exp(-diff))
        labels[m] = 1 if rng.random() < prob else 0
        rows.append(([ids[i] for i in ally_idx], [ids[i] for i in enemy_idx]))

    return ids, rows, labels
