package gg.leaguetool.profile.model;

import java.util.List;

/** Aggregated player profile returned by the profile endpoint. */
public record PlayerProfile(
        String puuid,
        String gameName,
        String tagLine,
        String platform,
        long summonerLevel,
        int profileIconId,
        List<RankedStats> ranked,
        List<ChampionMasterySummary> topMastery,
        List<MatchSummary> recentMatches) {
}
