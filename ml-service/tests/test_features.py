from app.features import FeatureBuilder


def test_multihot_encoding_places_allies_and_enemies_in_separate_blocks():
    builder = FeatureBuilder([10, 20, 30])
    vector = builder.transform_one([10, 30], [20])

    assert vector.shape[0] == 6  # 2 * 3 champions
    assert vector[0] == 1.0  # ally 10 -> index 0
    assert vector[2] == 1.0  # ally 30 -> index 2
    assert vector[3 + 1] == 1.0  # enemy 20 -> n + index 1
    assert vector.sum() == 3.0


def test_unknown_champions_are_ignored():
    builder = FeatureBuilder([1, 2])
    vector = builder.transform_one([999], [1])
    assert vector.sum() == 1.0


def test_n_features_is_twice_the_universe():
    builder = FeatureBuilder([5, 6, 7, 8])
    assert builder.n_features == 8
