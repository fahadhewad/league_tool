package gg.leaguetool.form;

import gg.leaguetool.common.Platform;
import gg.leaguetool.form.model.FormFactor;
import gg.leaguetool.form.model.GameForm;
import gg.leaguetool.form.model.RecentForm;
import gg.leaguetool.profile.MatchHistoryService;
import gg.leaguetool.profile.model.MatchSummary;
import gg.leaguetool.riot.RiotApiClient;
import gg.leaguetool.riot.dto.AccountDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Computes a recent-form / "tilt" score from a player's last N games. The score is a transparent
 * weighted blend of win rate, KDA, a recent-vs-older trend, and the current win/loss streak; every
 * component is returned in the breakdown so the number can be explained.
 */
@Service
public class RecentFormService {

    public static final int DEFAULT_GAMES = 10;
    public static final int MIN_GAMES = 1;
    public static final int MAX_GAMES = 20;

    private static final double W_WIN_RATE = 0.40;
    private static final double W_KDA = 0.30;
    private static final double W_TREND = 0.15;
    private static final double W_STREAK = 0.15;

    private final RiotApiClient riot;
    private final MatchHistoryService matchHistory;

    public RecentFormService(RiotApiClient riot, MatchHistoryService matchHistory) {
        this.riot = riot;
        this.matchHistory = matchHistory;
    }

    public RecentForm analyze(Platform platform, String gameName, String tagLine, int games) {
        int n = Math.clamp(games, MIN_GAMES, MAX_GAMES);
        AccountDto account = riot.getAccountByRiotId(platform.accountRegion(), gameName, tagLine);
        List<MatchSummary> matches = matchHistory.recentMatches(platform, account.puuid(), n);
        return compute(account, platform, matches);
    }

    RecentForm compute(AccountDto account, Platform platform, List<MatchSummary> matches) {
        int analyzed = matches.size();
        if (analyzed == 0) {
            return new RecentForm(account.puuid(), account.gameName(), account.tagLine(), platform.host(),
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 50.0, "No recent games", false, List.of(), List.of());
        }

        int wins = (int) matches.stream().filter(MatchSummary::win).count();
        int losses = analyzed - wins;
        double winRate = round(100.0 * wins / analyzed, 1);

        double avgKills = avg(matches, MatchSummary::kills);
        double avgDeaths = avg(matches, MatchSummary::deaths);
        double avgAssists = avg(matches, MatchSummary::assists);
        double avgKda = round(matches.stream().mapToDouble(MatchSummary::kda).average().orElse(0), 2);
        double avgCsPerMin = round(matches.stream()
                .mapToDouble(m -> m.gameDurationSeconds() > 0
                        ? m.csTotal() / (m.gameDurationSeconds() / 60.0) : 0)
                .average().orElse(0), 2);

        int streak = currentStreak(matches);

        double recentKda = halfAverageKda(matches, true);
        double olderKda = halfAverageKda(matches, false);

        double winRateScore = winRate;
        double kdaScore = Math.clamp(avgKda / 5.0 * 100.0, 0, 100);
        double trendScore = analyzed < 4 ? 50.0 : Math.clamp(50.0 + (recentKda - olderKda) * 25.0, 0, 100);
        double streakScore = Math.clamp(50.0 + streak * 12.5, 0, 100);

        double formScore = round(winRateScore * W_WIN_RATE + kdaScore * W_KDA
                + trendScore * W_TREND + streakScore * W_STREAK, 1);

        boolean tiltAlert = streak <= -3 || (formScore < 35.0 && losses > wins);

        List<FormFactor> breakdown = List.of(
                new FormFactor("Win rate", round(winRateScore, 1), W_WIN_RATE,
                        wins + "W " + losses + "L over last " + analyzed + " (" + winRate + "%)"),
                new FormFactor("KDA", round(kdaScore, 1), W_KDA,
                        "Average KDA " + avgKda),
                new FormFactor("Trend", round(trendScore, 1), W_TREND,
                        analyzed < 4 ? "Not enough games to judge a trend"
                                : "Recent KDA " + round(recentKda, 2) + " vs earlier " + round(olderKda, 2)),
                new FormFactor("Streak", round(streakScore, 1), W_STREAK, streakDetail(streak)));

        List<GameForm> games = matches.stream()
                .map(m -> new GameForm(m.matchId(), m.championName(), m.queueId(),
                        m.win(), m.kills(), m.deaths(), m.assists(), m.kda()))
                .toList();

        return new RecentForm(account.puuid(), account.gameName(), account.tagLine(), platform.host(),
                analyzed, wins, losses, winRate, avgKda, round(avgKills, 1), round(avgDeaths, 1),
                round(avgAssists, 1), avgCsPerMin, streak, formScore, label(formScore), tiltAlert,
                breakdown, games);
    }

    /** Signed streak from the most recent game: positive for wins, negative for losses. */
    private static int currentStreak(List<MatchSummary> matches) {
        boolean firstWin = matches.get(0).win();
        int streak = 0;
        for (MatchSummary m : matches) {
            if (m.win() == firstWin) {
                streak++;
            } else {
                break;
            }
        }
        return firstWin ? streak : -streak;
    }

    /** Average KDA over the recent half (most-recent first) or the older half. */
    private static double halfAverageKda(List<MatchSummary> matches, boolean recent) {
        int half = matches.size() / 2;
        if (half == 0) {
            return matches.stream().mapToDouble(MatchSummary::kda).average().orElse(0);
        }
        List<MatchSummary> slice = recent
                ? matches.subList(0, half)
                : matches.subList(matches.size() - half, matches.size());
        return slice.stream().mapToDouble(MatchSummary::kda).average().orElse(0);
    }

    private static String streakDetail(int streak) {
        if (streak == 0) {
            return "No active streak";
        }
        return Math.abs(streak) + (streak > 0 ? "-game win streak" : "-game loss streak");
    }

    private static String label(double formScore) {
        if (formScore >= 75) {
            return "On fire";
        } else if (formScore >= 60) {
            return "Hot";
        } else if (formScore >= 45) {
            return "Steady";
        } else if (formScore >= 30) {
            return "Cooling off";
        }
        return "Tilted";
    }

    private static double avg(List<MatchSummary> matches, ToIntFunction<MatchSummary> field) {
        return matches.stream().mapToInt(field).average().orElse(0);
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
