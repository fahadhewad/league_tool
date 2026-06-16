package gg.leaguetool.form;

import gg.leaguetool.common.Platform;
import gg.leaguetool.form.model.FormFactor;
import gg.leaguetool.form.model.GameForm;
import gg.leaguetool.form.model.RecentForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecentFormController.class)
class RecentFormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecentFormService recentFormService;

    @Test
    void returnsRecentForm() throws Exception {
        RecentForm form = new RecentForm("PUUID-X", "Agurin", "EUW", "euw1",
                3, 1, 2, 33.3, 1.5, 3.0, 6.0, 5.0, 5.2, -2, 28.4, "Tilted", true,
                List.of(new FormFactor("Win rate", 33.3, 0.40, "1W 2L over last 3 (33.3%)")),
                List.of(new GameForm("EUW1_1", "LeeSin", 420, false, 2, 8, 3, 0.63)));
        when(recentFormService.analyze(eq(Platform.EUW1), eq("Agurin"), eq("EUW"), anyInt()))
                .thenReturn(form);

        mockMvc.perform(get("/api/v1/form/euw1/Agurin/EUW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formLabel").value("Tilted"))
                .andExpect(jsonPath("$.tiltAlert").value(true))
                .andExpect(jsonPath("$.currentStreak").value(-2))
                .andExpect(jsonPath("$.breakdown[0].factor").value("Win rate"));
    }

    @Test
    void rejectsInvalidGamesParam() throws Exception {
        mockMvc.perform(get("/api/v1/form/euw1/Agurin/EUW").param("games", "0"))
                .andExpect(status().isBadRequest());
    }
}
