package gg.leaguetool.draft;

import gg.leaguetool.champion.Role;

/**
 * Source of the numbers behind draft analysis: champion role win-rates, pairwise synergy, and
 * lane counter advantages. Values are in percentage points.
 *
 * <p>The seed implementation derives these heuristically from champion attributes so the analyzer
 * works offline. Once the match crawler has built aggregates, a database-backed provider can return
 * empirical rates through the same interface without changing the analyzer or recommender.
 */
public interface DraftDataProvider {

    /** Expected win rate for a champion in a role, e.g. {@code 50.0}. */
    double baseWinRate(int championId, Role role);

    /** Synergy between two allied champions as a win-rate lift in percentage points (may be negative). */
    double synergy(int championA, int championB);

    /** Advantage of {@code championId} against {@code vsChampionId} in percentage points (antisymmetric). */
    double counter(int championId, int vsChampionId);
}
