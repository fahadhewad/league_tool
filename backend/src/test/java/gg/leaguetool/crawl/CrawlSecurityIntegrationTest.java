package gg.leaguetool.crawl;

import gg.leaguetool.common.Platform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check that {@code AdminAuthFilter} actually gates the crawl endpoint in the wired
 * application (token configured here so the admin surface is enabled).
 */
@SpringBootTest(properties = "admin.api-token=test-secret")
@AutoConfigureMockMvc
class CrawlSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchCrawler crawler;

    @Test
    void rejectsCrawlWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawl").param("platform", "euw1").param("puuid", "abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsCrawlWithWrongToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/crawl")
                        .header("X-Admin-Token", "wrong")
                        .param("platform", "euw1").param("puuid", "abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsCrawlWithCorrectToken() throws Exception {
        when(crawler.crawl(eq(Platform.EUW1), anyString(), anyInt())).thenReturn(5);

        mockMvc.perform(post("/api/v1/admin/crawl")
                        .header("X-Admin-Token", "test-secret")
                        .param("platform", "euw1").param("puuid", "abc").param("maxMatches", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingested").value(5));
    }
}
