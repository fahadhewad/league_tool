package gg.leaguetool.profile.model;

/** Ranked standing for a single queue, with win-rate pre-computed for convenience. */
public record RankedStats(
        String queueType,
        String tier,
        String rank,
        int leaguePoints,
        int wins,
        int losses,
        double winRate,
        boolean hotStreak) {

    public static RankedStats from(gg.leaguetool.riot.dto.LeagueEntryDto e) {
        int games = e.wins() + e.losses();
        double wr = games == 0 ? 0.0 : Math.round((100.0 * e.wins() / games) * 10.0) / 10.0;
        return new RankedStats(e.queueType(), e.tier(), e.rank(), e.leaguePoints(),
                e.wins(), e.losses(), wr, e.hotStreak());
    }
}
