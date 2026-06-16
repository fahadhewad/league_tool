package gg.leaguetool.draft;

import gg.leaguetool.champion.Role;
import gg.leaguetool.crawl.MatchFixtures;
import gg.leaguetool.crawl.MatchIngestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ MatchIngestService.class, DbDraftDataProvider.class })
@TestPropertySource(properties = "draft.db.min-games=2")
class DbDraftDataProviderTest {

    @Autowired MatchIngestService ingest;
    @Autowired DbDraftDataProvider provider;

    @Test
    void computesEmpiricalRatesOnceEnoughGamesExist() {
        // Champions 1..10, blue (1..5) wins twice.
        ingest.ingest(MatchFixtures.standardMatch("EUW1_1", 1, true));
        ingest.ingest(MatchFixtures.standardMatch("EUW1_2", 1, true));

        assertThat(provider.baseWinRate(1, Role.TOP)).hasValue(100.0);
        assertThat(provider.synergy(1, 2)).hasValue(50.0); // pair win rate 100 → +50 lift
        assertThat(provider.counter(1, 6)).hasValue(50.0); // 1 beats 6 every game → +50
    }

    @Test
    void returnsEmptyBelowTheMinimumGamesThreshold() {
        ingest.ingest(MatchFixtures.standardMatch("EUW1_1", 1, true)); // only 1 game (< 2)
        assertThat(provider.baseWinRate(1, Role.TOP)).isEmpty();
        assertThat(provider.synergy(1, 2)).isEmpty();
    }
}
