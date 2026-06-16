package gg.leaguetool.draft;

import gg.leaguetool.champion.Role;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The {@link DraftDataProvider} the analyzer and recommender actually use: empirical numbers from
 * crawled matches when available, falling back to the heuristic {@link SeedDraftDataProvider} prior
 * otherwise. As the crawler accumulates data, advice shifts smoothly from heuristic to measured.
 */
@Component
@Primary
public class CompositeDraftDataProvider implements DraftDataProvider {

    private final DbDraftDataProvider db;
    private final SeedDraftDataProvider seed;

    public CompositeDraftDataProvider(DbDraftDataProvider db, SeedDraftDataProvider seed) {
        this.db = db;
        this.seed = seed;
    }

    @Override
    public double baseWinRate(int championId, Role role) {
        return db.baseWinRate(championId, role).orElseGet(() -> seed.baseWinRate(championId, role));
    }

    @Override
    public double synergy(int championA, int championB) {
        return db.synergy(championA, championB).orElseGet(() -> seed.synergy(championA, championB));
    }

    @Override
    public double counter(int championId, int vsChampionId) {
        return db.counter(championId, vsChampionId).orElseGet(() -> seed.counter(championId, vsChampionId));
    }
}
