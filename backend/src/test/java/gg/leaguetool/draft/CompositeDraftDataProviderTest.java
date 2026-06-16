package gg.leaguetool.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.leaguetool.champion.ChampionRepository;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeDraftDataProviderTest {

    private static final int LEONA = 89;
    private static final int JINX = 222;

    private final ChampionRepository repo = new ChampionRepository(new ObjectMapper());
    private final SeedDraftDataProvider seed = new SeedDraftDataProvider(repo);
    private final DbDraftDataProvider db = mock(DbDraftDataProvider.class);
    private final CompositeDraftDataProvider composite = new CompositeDraftDataProvider(db, seed);

    @Test
    void prefersEmpiricalDataWhenAvailable() {
        when(db.synergy(LEONA, JINX)).thenReturn(OptionalDouble.of(7.0));
        assertThat(composite.synergy(LEONA, JINX)).isEqualTo(7.0);
    }

    @Test
    void fallsBackToTheSeedHeuristicWhenDataIsInsufficient() {
        when(db.synergy(LEONA, JINX)).thenReturn(OptionalDouble.empty());
        // Seed heuristic value for this frontline+carry, mixed-damage pairing.
        assertThat(composite.synergy(LEONA, JINX)).isEqualTo(3.0);
    }
}
