package gg.leaguetool.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
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

        PickRecommendation top = recs.recommendations().get(0);
        assertThat(top.championId()).isEqualTo(LEONA);
        assertThat(top.score()).isBetween(60.0, 75.0);
        assertThat(top.reasons()).isNotEmpty();
        // All results are for the requested role and exclude already-picked champions.
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

        assertThat(recs.recommendations()).noneMatch(r -> r.championId() == LEONA);
        assertThat(recs.recommendations().get(0).championId()).isEqualTo(NAUTILUS);
    }

    @Test
    void worksWithNoContextAndReturnsNeutralScores() {
        PickRecommendations recs = recommender.recommend(Role.TOP, List.of(), List.of(), List.of());

        assertThat(recs.recommendations()).isNotEmpty();
        assertThat(recs.recommendations()).allMatch(r -> r.role() == Role.TOP);
        assertThat(recs.recommendations().get(0).score()).isEqualTo(50.0);
    }
}
