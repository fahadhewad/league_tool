package gg.leaguetool.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.leaguetool.champion.Champion;
import gg.leaguetool.champion.ChampionRepository;
import gg.leaguetool.champion.Role;
import gg.leaguetool.draft.model.DraftPick;
import gg.leaguetool.draft.model.PickRecommendation;
import gg.leaguetool.draft.model.PickRecommendations;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PickRecommenderServiceTest {

    private static final int LEONA = 89;
    private static final int NAUTILUS = 111;
    private static final int JINX = 222;
    private static final int ZED = 238;

    private final ChampionRepository repo = new ChampionRepository(new ObjectMapper());
    private final PickRecommenderService recommender =
            new PickRecommenderService(repo, new SeedDraftDataProvider(repo));

    @Test
    void recommendsAnEngageTankSupportForAnAdCarryTeam() {
        // Our team has an AD carry (Jinx); enemy has a fed-prone assassin (Zed). We pick support.
        PickRecommendations recs = recommender.recommend(
                Role.UTILITY,
                List.of(new DraftPick(JINX, Role.BOTTOM)),
                List.of(new DraftPick(ZED, Role.MIDDLE)),
                List.of());

        assertThat(recs.role()).isEqualTo(Role.UTILITY);
        assertThat(recs.recommendations()).isNotEmpty();

        // The full roster contains several equally-strong engage tank supports (Leona, Nautilus,
        // Blitzcrank, Thresh ...), so we assert the *profile* of the top pick rather than one
        // arbitrary tied champion: a front-line, CC support that is favoured into the assassin.
        PickRecommendation top = recs.recommendations().get(0);
        Champion topChampion = repo.byId(top.championId()).orElseThrow();
        assertThat(topChampion.isFrontline()).as("top pick is a front line").isTrue();
        assertThat(topChampion.cc()).as("top pick brings hard CC").isTrue();
        assertThat(top.score()).isBetween(60.0, 75.0);
        assertThat(top.counterScore()).as("favoured into the enemy assassin").isGreaterThanOrEqualTo(1.0);
        assertThat(top.reasons()).isNotEmpty();

        // Leona is a canonical answer and should surface among the (tied) best picks.
        assertThat(recs.recommendations()).anyMatch(r -> r.championId() == LEONA);
        assertThat(recs.recommendations()).allMatch(r -> r.role() == Role.UTILITY);
        assertThat(recs.recommendations()).noneMatch(r -> r.championId() == JINX || r.championId() == ZED);
    }

    @Test
    void respectsBans() {
        PickRecommendations recs = recommender.recommend(
                Role.UTILITY,
                List.of(new DraftPick(JINX, Role.BOTTOM)),
                List.of(new DraftPick(ZED, Role.MIDDLE)),
                List.of(LEONA));

        // Banned champion never appears; the next-best pick is still a strong engage tank support.
        assertThat(recs.recommendations()).noneMatch(r -> r.championId() == LEONA);
        PickRecommendation top = recs.recommendations().get(0);
        Champion topChampion = repo.byId(top.championId()).orElseThrow();
        assertThat(topChampion.isFrontline()).isTrue();
        assertThat(topChampion.cc()).isTrue();
        assertThat(top.score()).isBetween(60.0, 75.0);
        assertThat(recs.recommendations()).anyMatch(r -> r.championId() == NAUTILUS);
    }

    @Test
    void worksWithNoContextAndReturnsNeutralScores() {
        PickRecommendations recs = recommender.recommend(Role.TOP, List.of(), List.of(), List.of());

        assertThat(recs.recommendations()).isNotEmpty();
        assertThat(recs.recommendations()).allMatch(r -> r.role() == Role.TOP);
        assertThat(recs.recommendations().get(0).score()).isEqualTo(50.0);
    }
}
