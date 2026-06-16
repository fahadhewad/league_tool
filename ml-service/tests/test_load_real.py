import numpy as np

from training.load_real import build_dataset


def _match(match_id, ally_champs, enemy_champs, winning_team):
    rows = [(match_id, 100, c, winning_team) for c in ally_champs]
    rows += [(match_id, 200, c, winning_team) for c in enemy_champs]
    return rows


def test_groups_flat_rows_into_team_matchups_with_labels():
    records = []
    records += _match("EUW1_1", [1, 2, 3, 4, 5], [6, 7, 8, 9, 10], winning_team=100)
    records += _match("EUW1_2", [1, 2, 3, 4, 5], [11, 12, 13, 14, 15], winning_team=200)

    champion_ids, rows, labels = build_dataset(records)

    assert len(rows) == 2
    assert sorted(champion_ids) == champion_ids  # sorted, de-duplicated
    assert 15 in champion_ids
    # Label is whether team 100 (allies) won.
    by_match = {tuple(a): lbl for (a, _e), lbl in zip(rows, labels)}
    assert labels.dtype == np.int8
    assert by_match[(1, 2, 3, 4, 5)] in (0, 1)
    assert set(labels.tolist()) == {0, 1}


def test_drops_incomplete_or_unfinished_matches():
    records = []
    records += _match("full", [1, 2, 3, 4, 5], [6, 7, 8, 9, 10], winning_team=100)
    # Only four allies -> not a 5v5, must be dropped.
    records += _match("short", [1, 2, 3, 4], [6, 7, 8, 9, 10], winning_team=200)
    # Unknown winner -> dropped.
    records += _match("nowinner", [1, 2, 3, 4, 5], [6, 7, 8, 9, 10], winning_team=None)

    _ids, rows, labels = build_dataset(records)

    assert len(rows) == 1
    assert len(labels) == 1
