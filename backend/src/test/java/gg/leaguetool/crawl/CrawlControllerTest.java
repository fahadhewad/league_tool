package gg.leaguetool.crawl;

import gg.leaguetool.common.Platform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CrawlController.class)
class CrawlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchCrawler crawler;

    @Test
    void triggersABoundedCrawl() throws Exception {
        when(crawler.crawl(eq(Platform.EUW1), eq("abc"), anyInt())).thenReturn(7);

        mockMvc.perform(post("/api/v1/admin/crawl")
                        .param("platform", "euw1").param("puuid", "abc").param("maxMatches", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingested").value(7))
                .andExpect(jsonPath("$.requested").value(10));
    }

    @Test
    void rejectsInvalidMaxMatches() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawl")
                        .param("platform", "euw1").param("puuid", "abc").param("maxMatches", "0"))
                .andExpect(status().isBadRequest());
    }
}
