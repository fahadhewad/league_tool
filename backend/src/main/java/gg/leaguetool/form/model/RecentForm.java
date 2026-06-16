package gg.leaguetool.form.model;

import java.util.List;

/**
 * Recent-form / "tilt" assessment for a player over their last N games, with a fully transparent
 * breakdown so the score can be explained rather than taken on faith.
 *
 * @param formScore     composite 0–100 score
 * @param formLabel     human label derived from {@code formScore}
 * @param tiltAlert     true when recent results suggest the player may be tilted
 * @param currentStreak signed streak from the most recent game (positive = wins, negative = losses)
 * @param breakdown     per-factor contributions to {@code formScore}
 * @param games         per-game detail (most-recent first)
 */
public record RecentForm(
        String puuid,
        String gameName,
        String tagLine,
        String platform,
        int gamesAnalyzed,
        int wins,
        int losses,
        double winRate,
        double avgKda,
        double avgKills,
        double avgDeaths,
        double avgAssists,
        double avgCsPerMin,
        int currentStreak,
        double formScore,
        String formLabel,
        boolean tiltAlert,
        List<FormFactor> breakdown,
        List<GameForm> games) {
}
