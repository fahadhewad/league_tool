"""Load real training data from the crawled Match-V5 corpus in Postgres.

The backend's crawler fills ``matches`` (with the winning team) and ``match_participant`` (one row
per player with champion + team). This turns those into the same ``(champion_ids, rows, labels)``
shape the synthetic generator produces, so the training pipeline is identical either way.

By convention team 100 is treated as "allies" and the label is whether team 100 won; the model is
symmetric in allies/enemies via featurization, so the choice is arbitrary but consistent.
"""
from __future__ import annotations

import os
from collections import defaultdict
from collections.abc import Iterable

import numpy as np

from app.config import TEAM_SIZE

ALLY_TEAM = 100
ENEMY_TEAM = 200


def default_dsn() -> str:
    """Connection string from LEAGUETOOL_DB_URL, or assembled from POSTGRES_* env vars."""
    url = os.environ.get("LEAGUETOOL_DB_URL")
    if url:
        return url
    host = os.environ.get("POSTGRES_HOST", "localhost")
    port = os.environ.get("POSTGRES_PORT", "5432")
    db = os.environ.get("POSTGRES_DB", "leaguetool")
    user = os.environ.get("POSTGRES_USER", "leaguetool")
    password = os.environ.get("POSTGRES_PASSWORD", "leaguetool")
    return f"postgresql://{user}:{password}@{host}:{port}/{db}"


def build_dataset(records: Iterable[tuple[str, int, int, int]], team_size: int = TEAM_SIZE):
    """Group flat ``(match_id, team_id, champion_id, winning_team)`` rows into training examples.

    Only complete ``team_size``-vs-``team_size`` matches with a known winner are kept.
    Returns ``(champion_ids, rows, labels)`` matching ``synthesize.generate``.
    """
    teams: dict[str, dict[int, list[int]]] = defaultdict(lambda: {ALLY_TEAM: [], ENEMY_TEAM: []})
    winners: dict[str, int] = {}

    for match_id, team_id, champion_id, winning_team in records:
        if team_id in (ALLY_TEAM, ENEMY_TEAM):
            teams[match_id][team_id].append(int(champion_id))
            winners[match_id] = winning_team

    champion_ids: set[int] = set()
    rows: list[tuple[list[int], list[int]]] = []
    labels: list[int] = []

    for match_id, sides in teams.items():
        allies, enemies = sides[ALLY_TEAM], sides[ENEMY_TEAM]
        if len(allies) == team_size and len(enemies) == team_size and winners.get(match_id) is not None:
            rows.append((allies, enemies))
            labels.append(1 if winners[match_id] == ALLY_TEAM else 0)
            champion_ids.update(allies)
            champion_ids.update(enemies)

    return sorted(champion_ids), rows, np.asarray(labels, dtype=np.int8)


def load_from_postgres(dsn: str | None = None):
    """Read the crawled corpus from Postgres and build a training dataset."""
    import psycopg2  # imported lazily so the dependency is only needed for real training

    dsn = dsn or default_dsn()
    query = (
        "SELECT mp.match_id, mp.team_id, mp.champion_id, m.winning_team "
        "FROM match_participant mp "
        "JOIN matches m ON m.match_id = mp.match_id "
        "WHERE m.winning_team IS NOT NULL"
    )
    conn = psycopg2.connect(dsn)
    try:
        with conn.cursor() as cur:
            cur.execute(query)
            records = cur.fetchall()
    finally:
        conn.close()
    return build_dataset(records)
