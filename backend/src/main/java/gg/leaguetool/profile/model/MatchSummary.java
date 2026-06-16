package gg.leaguetool.profile.model;

import gg.leaguetool.riot.dto.MatchDto;

/** One row of match history from the perspective of a specific player. */
public record MatchSummary(
        String matchId,
        int queueId,
        String gameMode,
        String championName,
        String teamPosition,
        boolean win,
        int kills,
        int deaths,
        int assists,
        double kda,
        long gameDurationSeconds,
        long gameEndTimestamp,
        int damageToChampions,
        int goldEarned,
        int csTotal) {

    /** Builds a summary for {@code puuid} from a full match, or {@code null} if not a participant. */
    public static MatchSummary forPlayer(MatchDto match, String puuid) {
        MatchDto.Info info = match.info();
        if (info == null || info.participants() == null) {
            return null;
        }
        MatchDto.Participant p = info.participants().stream()
                .filter(x -> puuid.equals(x.puuid()))
                .findFirst()
                .orElse(null);
        if (p == null) {
            return null;
        }
        double kda = p.deaths() == 0
                ? (p.kills() + p.assists())
                : Math.round(((double) (p.kills() + p.assists()) / p.deaths()) * 100.0) / 100.0;
        return new MatchSummary(
                match.metadata() == null ? null : match.metadata().matchId(),
                info.queueId(),
                info.gameMode(),
                p.championName(),
                p.teamPosition(),
                p.win(),
                p.kills(),
                p.deaths(),
                p.assists(),
                kda,
                info.gameDuration(),
                info.gameEndTimestamp(),
                p.totalDamageDealtToChampions(),
                p.goldEarned(),
                p.totalMinionsKilled() + p.neutralMinionsKilled());
    }
}
