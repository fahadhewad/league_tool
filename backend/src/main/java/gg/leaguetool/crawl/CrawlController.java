package gg.leaguetool.crawl;

import gg.leaguetool.common.Platform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin trigger for a bounded match crawl. This is an operator endpoint and drives outbound Riot
 * traffic, so it sits under {@code /api/v1/admin} and is gated by the admin-token filter
 * ({@code AdminAuthFilter}); it is disabled entirely unless {@code ADMIN_API_TOKEN} is set.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Operational endpoints (crawler)")
public class CrawlController {

    private final MatchCrawler crawler;

    public CrawlController(MatchCrawler crawler) {
        this.crawler = crawler;
    }

    @PostMapping("/crawl")
    @Operation(summary = "Crawl matches starting from a seed player (bounded)")
    public CrawlResult crawl(
            @RequestParam String platform,
            @RequestParam @NotBlank String puuid,
            @RequestParam(defaultValue = "50") @Min(1) @Max(50000) int maxMatches) {
        Platform p = Platform.fromString(platform);
        int ingested = crawler.crawl(p, puuid, maxMatches);
        return new CrawlResult(ingested, maxMatches);
    }

    public record CrawlResult(int ingested, int requested) {
    }
}
