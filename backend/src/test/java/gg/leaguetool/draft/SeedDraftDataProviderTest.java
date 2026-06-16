package gg.leaguetool.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.leaguetool.champion.ChampionRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeedDraftDataProviderTest {

    private static final int LEONA = 89;
    private static final int JINX = 222;
    private static final int ZED = 238;
    private static final int SYNDRA = 134;
    private static final int MALPHITE = 54;

    private final ChampionRepository repo = new ChampionRepository(new ObjectMapper());
    private final SeedDraftDataProvider data = new SeedDraftDataProvider(repo);

    @Test
    void rewardsFrontlinePlusCarryWithMixedDamage() {
        // Leona (tank, magic, CC) + Jinx (marksman, physical): caps at +3.0.
        assertThat(data.synergy(LEONA, JINX)).isEqualTo(3.0);
        assertThat(data.synergy(JINX, LEONA)).isEqualTo(3.0); // symmetric
    }

    @Test
    void assassinIsFavouredIntoSquishyMageAndCounterIsAntisymmetric() {
        assertThat(data.counter(ZED, SYNDRA)).isEqualTo(1.5);
        assertThat(data.counter(SYNDRA, ZED)).isEqualTo(-1.5);
    }

    @Test
    void tankIsStronglyFavouredIntoAssassin() {
        assertThat(data.counter(MALPHITE, ZED)).isEqualTo(3.0);
    }

    @Test
    void unknownOrIdenticalChampionsReturnZero() {
        assertThat(data.synergy(LEONA, LEONA)).isZero();
        assertThat(data.counter(LEONA, 999999)).isZero();
    }
}
