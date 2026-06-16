package gg.leaguetool.riot.dto;

/** League-V4 ranked entry for a single queue (e.g. RANKED_SOLO_5x5). */
public record LeagueEntryDto(
        String leagueId,
        String queueType,
        String tier,
        String rank,
        String puuid,
        String summonerId,
        int leaguePoints,
        int wins,
        int losses,
        boolean veteran,
        boolean inactive,
        boolean freshBlood,
        boolean hotStreak) {
}
