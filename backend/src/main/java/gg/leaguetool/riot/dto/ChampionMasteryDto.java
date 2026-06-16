package gg.leaguetool.riot.dto;

/** Champion-Mastery-V4 entry for one champion. */
public record ChampionMasteryDto(
        String puuid,
        int championId,
        int championLevel,
        long championPoints,
        long lastPlayTime,
        long championPointsSinceLastLevel,
        long championPointsUntilNextLevel,
        boolean chestGranted,
        int tokensEarned) {
}
