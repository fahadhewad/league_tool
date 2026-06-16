package gg.leaguetool.riot;

import gg.leaguetool.common.Platform;
import gg.leaguetool.common.Region;
import gg.leaguetool.common.error.RateLimitedException;
import gg.leaguetool.common.error.ResourceNotFoundException;
import gg.leaguetool.common.error.RiotApiException;
import gg.leaguetool.config.CacheConfig;
import gg.leaguetool.config.RiotApiProperties;
import gg.leaguetool.riot.dto.AccountDto;
import gg.leaguetool.riot.dto.ChampionMasteryDto;
import gg.leaguetool.riot.dto.LeagueEntryDto;
import gg.leaguetool.riot.dto.MatchDto;
import gg.leaguetool.riot.dto.SummonerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

/**
 * Low-level gateway to the Riot API. Every call is rate-limited, retried on transient failures, and
 * cached. Routing (regional vs platform host) is handled here so higher layers only pass a
 * {@link Region}/{@link Platform} plus identifiers.
 */
@Component
public class RiotApiClient {

    private static final Logger log = LoggerFactory.getLogger(RiotApiClient.class);
    private static final ParameterizedTypeReference<List<LeagueEntryDto>> LEAGUE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ChampionMasteryDto>> MASTERY_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<String>> STRING_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient riot;
    private final RateLimiter rateLimiter;
    private final RiotApiProperties props;

    public RiotApiClient(RestClient riotRestClient, RateLimiter rateLimiter, RiotApiProperties props) {
        this.riot = riotRestClient;
        this.rateLimiter = rateLimiter;
        this.props = props;
    }

    // ---- Account-V1 (regional) ----

    @Cacheable(cacheNames = CacheConfig.ACCOUNT, key = "#region + ':' + #gameName + ':' + #tagLine")
    public AccountDto getAccountByRiotId(Region region, String gameName, String tagLine) {
        URI uri = base(region.host())
                .pathSegment("riot", "account", "v1", "accounts", "by-riot-id", gameName, tagLine)
                .build().encode().toUri();
        return execute(() -> riot.get().uri(uri).retrieve().body(AccountDto.class),
                "account " + gameName + "#" + tagLine);
    }

    // ---- Summoner-V4 (platform) ----

    @Cacheable(cacheNames = CacheConfig.SUMMONER, key = "#platform + ':' + #puuid")
    public SummonerDto getSummonerByPuuid(Platform platform, String puuid) {
        URI uri = base(platform.host())
                .pathSegment("lol", "summoner", "v4", "summoners", "by-puuid", puuid)
                .build().encode().toUri();
        return execute(() -> riot.get().uri(uri).retrieve().body(SummonerDto.class),
                "summoner " + puuid);
    }

    // ---- League-V4 (platform) ----

    @Cacheable(cacheNames = CacheConfig.LEAGUE, key = "#platform + ':' + #puuid")
    public List<LeagueEntryDto> getLeagueEntriesByPuuid(Platform platform, String puuid) {
        URI uri = base(platform.host())
                .pathSegment("lol", "league", "v4", "entries", "by-puuid", puuid)
                .build().encode().toUri();
        return execute(() -> riot.get().uri(uri).retrieve().body(LEAGUE_LIST),
                "league entries " + puuid);
    }

    // ---- Champion-Mastery-V4 (platform) ----

    @Cacheable(cacheNames = CacheConfig.MASTERY, key = "#platform + ':' + #puuid")
    public List<ChampionMasteryDto> getChampionMasteries(Platform platform, String puuid) {
        URI uri = base(platform.host())
                .pathSegment("lol", "champion-mastery", "v4", "champion-masteries", "by-puuid", puuid)
                .build().encode().toUri();
        return execute(() -> riot.get().uri(uri).retrieve().body(MASTERY_LIST),
                "champion mastery " + puuid);
    }

    // ---- Match-V5 (regional) ----

    @Cacheable(cacheNames = CacheConfig.MATCH_IDS, key = "#region + ':' + #puuid + ':' + #start + ':' + #count")
    public List<String> getMatchIds(Region region, String puuid, int start, int count) {
        return getMatchIds(region, puuid, start, count, null);
    }

    /**
     * Match ids for a player, optionally filtered by Match-V5 {@code type} (e.g. {@code ranked}).
     * The crawler passes {@code ranked} so it builds a Summoner's Rift corpus rather than wandering
     * into Arena/ARAM games.
     */
    @Cacheable(cacheNames = CacheConfig.MATCH_IDS,
            key = "#region + ':' + #puuid + ':' + #start + ':' + #count + ':' + #type")
    public List<String> getMatchIds(Region region, String puuid, int start, int count, String type) {
        UriComponentsBuilder builder = base(region.host())
                .pathSegment("lol", "match", "v5", "matches", "by-puuid", puuid, "ids")
                .queryParam("start", start)
                .queryParam("count", count);
        if (type != null && !type.isBlank()) {
            builder.queryParam("type", type);
        }
        URI uri = builder.build().encode().toUri();
        return execute(() -> riot.get().uri(uri).retrieve().body(STRING_LIST),
                "match ids " + puuid);
    }

    @Cacheable(cacheNames = CacheConfig.MATCH, key = "#region + ':' + #matchId")
    public MatchDto getMatch(Region region, String matchId) {
        URI uri = base(region.host())
                .pathSegment("lol", "match", "v5", "matches", matchId)
                .build().encode().toUri();
        return execute(() -> riot.get().uri(uri).retrieve().body(MatchDto.class),
                "match " + matchId);
    }

    // ---- internals ----

    private UriComponentsBuilder base(String routeSegment) {
        return UriComponentsBuilder.fromUriString(String.format(props.baseUrlTemplate(), routeSegment));
    }

    /**
     * Executes a Riot call under the rate limiter, mapping upstream failures to domain exceptions
     * and retrying {@code 429}/{@code 5xx}/network errors with exponential backoff.
     */
    private <T> T execute(Supplier<T> call, String resource) {
        RiotApiProperties.Retry retry = props.retry();
        long backoff = retry.backoffMs();
        for (int attempt = 1; ; attempt++) {
            rateLimiter.acquire();
            try {
                return call.get();
            } catch (HttpClientErrorException.NotFound e) {
                throw new ResourceNotFoundException(resource + " not found");
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt >= retry.maxAttempts()) {
                    throw new RateLimitedException("Rate limited by Riot while fetching " + resource);
                }
                long waitMs = Math.min(Math.max(retryAfterMs(e), backoff), retry.maxBackoffMs());
                log.warn("429 from Riot for {} (attempt {}/{}); backing off {}ms",
                        resource, attempt, retry.maxAttempts(), waitMs);
                sleep(waitMs);
                backoff = Math.min(backoff * 2, retry.maxBackoffMs());
            } catch (HttpServerErrorException | ResourceAccessException e) {
                if (attempt >= retry.maxAttempts()) {
                    int status = e instanceof HttpServerErrorException h ? h.getStatusCode().value() : 502;
                    throw new RiotApiException("Upstream failure fetching " + resource + ": " + e.getMessage(),
                            status, e);
                }
                long waitMs = Math.min(backoff, retry.maxBackoffMs());
                log.warn("Transient error from Riot for {} (attempt {}/{}); retrying in {}ms: {}",
                        resource, attempt, retry.maxAttempts(), waitMs, e.getMessage());
                sleep(waitMs);
                backoff = Math.min(backoff * 2, retry.maxBackoffMs());
            } catch (HttpClientErrorException e) {
                // 400/401/403 etc. — not retryable (bad key, bad request).
                throw new RiotApiException("Client error fetching " + resource + ": "
                        + e.getStatusCode().value(), e.getStatusCode().value(), e);
            }
        }
    }

    private static long retryAfterMs(HttpClientErrorException e) {
        String header = e.getResponseHeaders() == null ? null
                : e.getResponseHeaders().getFirst("Retry-After");
        if (header == null) {
            return 0L;
        }
        try {
            return Long.parseLong(header.trim()) * 1000L;
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(Math.max(1, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Riot API backoff", e);
        }
    }
}
