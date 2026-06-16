package gg.leaguetool.profile;

import gg.leaguetool.common.Platform;
import gg.leaguetool.common.error.ResourceNotFoundException;
import gg.leaguetool.profile.model.ChampionMasterySummary;
import gg.leaguetool.profile.model.MatchSummary;
import gg.leaguetool.profile.model.PlayerProfile;
import gg.leaguetool.profile.model.RankedStats;
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

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    private static PlayerProfile sampleProfile() {
        return new PlayerProfile(
                "PUUID-AGURIN-0001", "Agurin", "EUW", "euw1", 712, 4567,
                List.of(new RankedStats("RANKED_SOLO_5x5", "CHALLENGER", "I", 845, 320, 250, 56.1, true)),
                List.of(new ChampionMasterySummary(121, null, 7, 523000)),
                List.of(new MatchSummary("EUW1_6543210001", 420, "CLASSIC", "LeeSin", "JUNGLE",
                        true, 8, 3, 14, 7.33, 1834, 1700001834000L, 21500, 13400, 210)));
    }

    @Test
    void returnsProfile() throws Exception {
        when(profileService.getProfile(eq(Platform.EUW1), eq("Agurin"), eq("EUW"), anyInt()))
                .thenReturn(sampleProfile());

        mockMvc.perform(get("/api/v1/profiles/euw1/Agurin/EUW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puuid").value("PUUID-AGURIN-0001"))
                .andExpect(jsonPath("$.ranked[0].queueType").value("RANKED_SOLO_5x5"))
                .andExpect(jsonPath("$.ranked[0].winRate").value(56.1))
                .andExpect(jsonPath("$.recentMatches[0].championName").value("LeeSin"));
    }

    @Test
    void rejectsInvalidMatchCount() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/euw1/Agurin/EUW").param("matchCount", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownPlatform() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/zzz/Agurin/EUW"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        when(profileService.getProfile(eq(Platform.EUW1), eq("Ghost"), eq("EUW"), anyInt()))
                .thenThrow(new ResourceNotFoundException("account Ghost#EUW not found"));

        mockMvc.perform(get("/api/v1/profiles/euw1/Ghost/EUW"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
