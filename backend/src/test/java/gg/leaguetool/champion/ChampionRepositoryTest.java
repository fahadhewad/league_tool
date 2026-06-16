package gg.leaguetool.champion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChampionRepositoryTest {

    private final ChampionRepository repository = new ChampionRepository(new ObjectMapper());

    @Test
    void bundledSeedCoversTheFullRoster() {
        // The generated seed is the whole roster, not a 39-champion sample, so the draft tools never
        // hit an unknown champion id during a real game.
        assertThat(repository.all().size()).isGreaterThanOrEqualTo(160);
    }

    @Test
    void resolvesRecentChampionsThatWereNotInTheOldCuratedSeed() {
        // These ids only exist because the seed is now Data-Dragon-complete.
        assertThat(repository.byId(901)).isPresent(); // Smolder
        assertThat(repository.byId(950)).isPresent(); // Naafiri
        assertThat(repository.byId(233)).isPresent(); // Briar
    }

    @Test
    void everyChampionHasAtLeastOneRoleAndTags() {
        for (Champion c : repository.all()) {
            assertThat(c.roles()).as("roles for %s", c.name()).isNotEmpty();
            assertThat(c.name()).isNotBlank();
            assertThat(c.damageType()).isNotNull();
        }
    }

    @Test
    void refreshAtomicallyReplacesTheRoster() {
        repository.refresh(List.of(
                new Champion(1, "Annie", List.of(Role.MIDDLE), DamageType.MAGIC, List.of(ChampionTag.MAGE), true)));
        assertThat(repository.all()).hasSize(1);
        assertThat(repository.byId(1)).isPresent();
        assertThat(repository.byName("annie")).isPresent();
    }
}
