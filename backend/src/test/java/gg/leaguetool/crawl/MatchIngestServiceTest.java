package gg.leaguetool.crawl;

import gg.leaguetool.persistence.ChampionMatchupStatRepository;
import gg.leaguetool.persistence.ChampionPairStat;
import gg.leaguetool.persistence.ChampionPairStatRepository;
import gg.leaguetool.persistence.ChampionRoleStatRepository;
import gg.leaguetool.persistence.MatchParticipantRepository;
import gg.leaguetool.persistence.MatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MatchIngestService.class)
class MatchIngestServiceTest {

    @Autowired MatchIngestService ingest;
    @Autowired MatchRepository matches;
    @Autowired MatchParticipantRepository participants;
    @Autowired ChampionRoleStatRepository roleStats;
    @Autowired ChampionPairStatRepository pairStats;
    @Autowired ChampionMatchupStatRepository matchupStats;

    @Test
    void ingestsMatchAndUpdatesAggregates() {
        boolean ingested = ingest.ingest(MatchFixtures.standardMatch("EUW1_1", 1, true));

        assertThat(ingested).isTrue();
        assertThat(matches.count()).isEqualTo(1);
        assertThat(participants.findByMatchId("EUW1_1")).hasSize(10);

        // Champion 1 played TOP on the winning blue team.
        assertThat(roleStats.findByChampionIdAndRole(1, "TOP")).hasValueSatisfying(s -> {
            assertThat(s.getGames()).isEqualTo(1);
            assertThat(s.winRate()).isEqualTo(100.0);
        });
        // Champion 6 played TOP on the losing red team.
        assertThat(roleStats.findByChampionIdAndRole(6, "TOP")).hasValueSatisfying(s ->
                assertThat(s.winRate()).isEqualTo(0.0));

        // Same-team pair (1,2) recorded with the win.
        assertThat(pairStats.findByChampionLowAndChampionHigh(ChampionPairStat.low(1, 2), ChampionPairStat.high(1, 2)))
                .hasValueSatisfying(s -> assertThat(s.winRate()).isEqualTo(100.0));

        // Lane matchup 1 vs 6 (TOP): champion 1 won.
        assertThat(matchupStats.totalGames(1, 6)).isEqualTo(1);
        assertThat(matchupStats.totalWins(1, 6)).isEqualTo(1);
        assertThat(matchupStats.totalWins(6, 1)).isEqualTo(0);
    }

    @Test
    void isIdempotentAndAccumulatesAcrossMatches() {
        ingest.ingest(MatchFixtures.standardMatch("EUW1_1", 1, true));
        boolean again = ingest.ingest(MatchFixtures.standardMatch("EUW1_1", 1, true));
        assertThat(again).isFalse();
        assertThat(matches.count()).isEqualTo(1);

        ingest.ingest(MatchFixtures.standardMatch("EUW1_2", 1, false)); // same champs, blue loses
        assertThat(roleStats.findByChampionIdAndRole(1, "TOP")).hasValueSatisfying(s -> {
            assertThat(s.getGames()).isEqualTo(2);
            assertThat(s.winRate()).isEqualTo(50.0);
        });
    }
}
