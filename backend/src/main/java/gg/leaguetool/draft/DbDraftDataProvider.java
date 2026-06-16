package gg.leaguetool.draft;

import gg.leaguetool.champion.Role;
import gg.leaguetool.persistence.ChampionMatchupStatRepository;
import gg.leaguetool.persistence.ChampionPairStat;
import gg.leaguetool.persistence.ChampionPairStatRepository;
import gg.leaguetool.persistence.ChampionRoleStatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.OptionalDouble;

/**
 * Empirical draft data computed from crawled matches. Each method returns a value only when there is
 * enough data (>= {@code draft.db.min-games}); otherwise it returns empty so the
 * {@link CompositeDraftDataProvider} can fall back to the heuristic seed.
 *
 * <p>Synergy and counter are expressed as win-rate lift in percentage points (rate - 50), matching
 * the seed provider's units.
 */
@Component
public class DbDraftDataProvider {

    private final ChampionRoleStatRepository roleStats;
    private final ChampionPairStatRepository pairStats;
    private final ChampionMatchupStatRepository matchupStats;
    private final long minGames;

    public DbDraftDataProvider(ChampionRoleStatRepository roleStats,
                               ChampionPairStatRepository pairStats,
                               ChampionMatchupStatRepository matchupStats,
                               @Value("${draft.db.min-games:20}") long minGames) {
        this.roleStats = roleStats;
        this.pairStats = pairStats;
        this.matchupStats = matchupStats;
        this.minGames = minGames;
    }

    public OptionalDouble baseWinRate(int championId, Role role) {
        return roleStats.findByChampionIdAndRole(championId, role.name())
                .filter(s -> s.getGames() >= minGames)
                .map(s -> OptionalDouble.of(round1(s.winRate())))
                .orElseGet(OptionalDouble::empty);
    }

    public OptionalDouble synergy(int championA, int championB) {
        if (championA == championB) {
            return OptionalDouble.empty();
        }
        int low = ChampionPairStat.low(championA, championB);
        int high = ChampionPairStat.high(championA, championB);
        return pairStats.findByChampionLowAndChampionHigh(low, high)
                .filter(s -> s.getGames() >= minGames)
                .map(s -> OptionalDouble.of(round1(s.winRate() - 50.0)))
                .orElseGet(OptionalDouble::empty);
    }

    public OptionalDouble counter(int championId, int vsChampionId) {
        if (championId == vsChampionId) {
            return OptionalDouble.empty();
        }
        long games = matchupStats.totalGames(championId, vsChampionId);
        if (games < minGames) {
            return OptionalDouble.empty();
        }
        long wins = matchupStats.totalWins(championId, vsChampionId);
        return OptionalDouble.of(round1(100.0 * wins / games - 50.0));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
