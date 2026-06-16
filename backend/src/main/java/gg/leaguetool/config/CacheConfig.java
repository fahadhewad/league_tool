package gg.leaguetool.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * In-process Caffeine caches for Riot lookups. TTLs reflect how often each resource changes:
 * matches are immutable (long TTL), ranked/mastery change slowly (short TTL).
 *
 * <p>A distributed Redis cache can be layered on later for multi-instance deployments; the cache
 * names stay the same so call sites are unaffected.
 */
@Configuration
public class CacheConfig {

    public static final String ACCOUNT = "riot-account";
    public static final String SUMMONER = "riot-summoner";
    public static final String LEAGUE = "riot-league";
    public static final String MASTERY = "riot-mastery";
    public static final String MATCH = "riot-match";
    public static final String MATCH_IDS = "riot-match-ids";

    @Bean
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                cache(ACCOUNT, Duration.ofHours(24), 50_000),
                cache(SUMMONER, Duration.ofHours(6), 50_000),
                cache(LEAGUE, Duration.ofMinutes(10), 50_000),
                cache(MASTERY, Duration.ofMinutes(30), 50_000),
                cache(MATCH, Duration.ofDays(30), 100_000),
                cache(MATCH_IDS, Duration.ofMinutes(5), 50_000)));
        return manager;
    }

    private static CaffeineCache cache(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}
