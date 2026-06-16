package gg.leaguetool.riot.dto;

import java.util.List;

/**
 * Match-V5 match details (subset). Only the fields LeagueTool uses are modelled; unknown fields are
 * ignored during deserialization.
 */
public record MatchDto(Metadata metadata, Info info) {

    public record Metadata(String dataVersion, String matchId, List<String> participants) {
    }

    public record Info(
            long gameCreation,
            long gameDuration,
            long gameStartTimestamp,
            long gameEndTimestamp,
            int queueId,
            String gameVersion,
            String gameMode,
            String gameType,
            List<Participant> participants) {
    }

    public record Participant(
            String puuid,
            String riotIdGameName,
            String riotIdTagline,
            int championId,
            String championName,
            String teamPosition,
            String individualPosition,
            int teamId,
            boolean win,
            int kills,
            int deaths,
            int assists,
            int totalDamageDealtToChampions,
            int totalMinionsKilled,
            int neutralMinionsKilled,
            int goldEarned,
            int champLevel,
            int visionScore) {
    }
}
