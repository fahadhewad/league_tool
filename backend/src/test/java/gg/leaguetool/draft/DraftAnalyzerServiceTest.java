package gg.leaguetool.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.leaguetool.champion.ChampionRepository;
import gg.leaguetool.champion.Role;
import gg.leaguetool.draft.model.DraftAnalysis;
import gg.leaguetool.draft.model.DraftPick;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DraftAnalyzerServiceTest {

    private final ChampionRepository repo = new ChampionRepository(new ObjectMapper());
    private final DraftAnalyzerService analyzer =
            new DraftAnalyzerService(repo, new SeedDraftDataProvider(repo));

    @Test
    void summarizesDamageBalanceAndCounterThreats() {
        // Allies: Garen (AD top), Jinx (AD bot). Enemy: Zed (assassin mid).
        List<DraftPick> allies = List.of(new DraftPick(86, Role.TOP), new DraftPick(222, Role.BOTTOM));
        List<DraftPick> enemies = List.of(new DraftPick(238, Role.MIDDLE));

        DraftAnalysis analysis = analyzer.analyze(allies, enemies);

        assertThat(analysis.allyComp().championCount()).isEqualTo(2);
        assertThat(analysis.allyComp().damageBalance()).isEqualTo("AD-heavy");
        assertThat(analysis.recommendations()).anyMatch(r -> r.contains("AD-heavy"));

        // Jinx is squishy into Zed → flagged as a threat; Garen (bruiser) is not.
        assertThat(analysis.threats()).hasSize(1);
        assertThat(analysis.threats().get(0).allyName()).isEqualTo("Jinx");
        assertThat(analysis.threats().get(0).enemyName()).isEqualTo("Zed");
    }

    @Test
    void flagsMissingFrontlineAndCrowdControl() {
        // Two squishy carries, no tank, only Jinx brings CC.
        List<DraftPick> allies = List.of(new DraftPick(222, Role.BOTTOM), new DraftPick(238, Role.MIDDLE));

        DraftAnalysis analysis = analyzer.analyze(allies, List.of());

        assertThat(analysis.allyComp().frontlineCount()).isZero();
        assertThat(analysis.recommendations()).anyMatch(r -> r.contains("front line"));
    }
}
