package gg.leaguetool.draft;

import gg.leaguetool.champion.Role;
import gg.leaguetool.draft.model.CompSummary;
import gg.leaguetool.draft.model.DraftAnalysis;
import gg.leaguetool.draft.model.PickRecommendation;
import gg.leaguetool.draft.model.PickRecommendations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DraftController.class)
class DraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DraftAnalyzerService analyzer;

    @MockitoBean
    private PickRecommenderService recommender;

    @Test
    void analyzeReturnsAnalysis() throws Exception {
        CompSummary comp = new CompSummary(2, 2, 0, 0, 1, 1, 1, "AD-heavy", List.of("AD-heavy damage profile"));
        when(analyzer.analyze(any(), any()))
                .thenReturn(new DraftAnalysis(comp, comp, 3.0, List.of("Garen + Jinx: +1.5 synergy"),
                        List.of(), List.of("Add a front line — your comp has no tank or bruiser to absorb damage.")));

        String body = "{\"allies\":[{\"championId\":86,\"role\":\"TOP\"}],\"enemies\":[]}";
        mockMvc.perform(post("/api/v1/draft/analyze").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allyComp.damageBalance").value("AD-heavy"))
                .andExpect(jsonPath("$.recommendations[0]").value(org.hamcrest.Matchers.containsString("front line")));
    }

    @Test
    void recommendReturnsRankedPicks() throws Exception {
        when(recommender.recommend(eq(Role.UTILITY), any(), any(), any()))
                .thenReturn(new PickRecommendations(Role.UTILITY, List.of(
                        new PickRecommendation(89, "Leona", Role.UTILITY, 68.0, 3.0, 3.0, 4.0,
                                List.of("Adds a front line your comp is missing")))));

        String body = "{\"role\":\"UTILITY\",\"allies\":[{\"championId\":222,\"role\":\"BOTTOM\"}],"
                + "\"enemies\":[],\"bans\":[]}";
        mockMvc.perform(post("/api/v1/draft/recommend").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("UTILITY"))
                .andExpect(jsonPath("$.recommendations[0].name").value("Leona"))
                .andExpect(jsonPath("$.recommendations[0].score").value(68.0));
    }

    @Test
    void recommendRequiresRole() throws Exception {
        String body = "{\"allies\":[],\"enemies\":[],\"bans\":[]}";
        mockMvc.perform(post("/api/v1/draft/recommend").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
