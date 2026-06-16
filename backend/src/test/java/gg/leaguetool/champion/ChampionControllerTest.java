package gg.leaguetool.champion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChampionController.class)
@Import(ChampionRepository.class)
class ChampionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAllChampions() throws Exception {
        mockMvc.perform(get("/api/v1/champions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(30)));
    }

    @Test
    void filtersByRole() throws Exception {
        mockMvc.perform(get("/api/v1/champions").param("role", "UTILITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[0].roles[0]").value("UTILITY"));
    }

    @Test
    void getsChampionById() throws Exception {
        mockMvc.perform(get("/api/v1/champions/64"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lee Sin"));
    }

    @Test
    void returns404ForUnknownChampion() throws Exception {
        mockMvc.perform(get("/api/v1/champions/999999"))
                .andExpect(status().isNotFound());
    }
}
