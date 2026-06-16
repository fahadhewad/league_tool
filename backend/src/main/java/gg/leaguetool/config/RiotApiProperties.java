package gg.leaguetool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the Riot API gateway, bound from {@code riot.api.*}.
 *
 * @param key               the Riot API key (from the {@code RIOT_API_KEY} env var; never hard-coded)
 * @param defaultPlatform   platform used when a request does not specify one (e.g. {@code euw1})
 * @param baseUrlTemplate   printf-style template for the routing base URL, e.g.
 *                          {@code https://%s.api.riotgames.com}. Tests override it to point at a
 *                          local stub server.
 * @param timeoutMs         per-request connect/read timeout in milliseconds
 * @param rateLimit         app-wide rate-limit budget
 * @param retry             retry/backoff behavior for transient failures
 */
@ConfigurationProperties(prefix = "riot.api")
public record RiotApiProperties(
        @DefaultValue("") String key,
        @DefaultValue("euw1") String defaultPlatform,
        @DefaultValue("https://%s.api.riotgames.com") String baseUrlTemplate,
        @DefaultValue("8000") int timeoutMs,
        @DefaultValue RateLimit rateLimit,
        @DefaultValue Retry retry) {

    /**
     * Application rate-limit budget. Defaults match a Riot development key
     * (20 requests/second and 100 requests/2 minutes).
     */
    public record RateLimit(
            @DefaultValue("20") int requestsPerSecond,
            @DefaultValue("100") int requestsPerTwoMinutes) {
    }

    /** Retry/backoff for {@code 429} and {@code 5xx} responses. */
    public record Retry(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("1000") long backoffMs,
            @DefaultValue("20000") long maxBackoffMs) {
    }

    public boolean hasKey() {
        return key != null && !key.isBlank();
    }
}
