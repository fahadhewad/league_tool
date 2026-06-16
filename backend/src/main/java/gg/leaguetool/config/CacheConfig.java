package gg.leaguetool.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Cache definitions for Riot lookups. TTLs reflect how often each resource changes: matches are
 * immutable (long TTL), ranked/mastery change slowly (short TTL).
 *
 * <p>The default {@link #cacheManager() Caffeine} manager is in-process. When {@code app.redis.enabled}
 * is true, {@link RedisConfig} provides a shared {@code RedisCacheManager} using the same
 * {@link #SPECS specs} so call sites and TTLs are identical across both backends.
 */
@Configuration
public class CacheConfig {

    public static final String ACCOUNT = "riot-account";
    public static final String SUMMONER = "riot-summoner";
    public static final String LEAGUE = "riot-league";
    public static final String MASTERY = "riot-mastery";
    public static final String MATCH = "riot-match";
    public static final String MATCH_IDS = "riot-match-ids";

    /** Shared cache specs, used to build either the Caffeine or the Redis cache manager. */
    public static final List<CacheSpec> SPECS = List.of(
            new CacheSpec(ACCOUNT, Duration.ofHours(24), 50_000),
            new CacheSpec(SUMMONER, Duration.ofHours(6), 50_000),
            new CacheSpec(LEAGUE, Duration.ofMinutes(10), 50_000),
            new CacheSpec(MASTERY, Duration.ofMinutes(30), 50_000),
            new CacheSpec(MATCH, Duration.ofDays(30), 100_000),
            new CacheSpec(MATCH_IDS, Duration.ofMinutes(5), 50_000));

    /** Name, time-to-live and (Caffeine-only) max entries for one cache. */
    public record CacheSpec(String name, Duration ttl, long maxSize) {
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(SPECS.stream().map(CacheConfig::caffeine).toList());
        return manager;
    }

    private static CaffeineCache caffeine(CacheSpec spec) {
        return new CaffeineCache(spec.name(), Caffeine.newBuilder()
                .expireAfterWrite(spec.ttl())
                .maximumSize(spec.maxSize())
                .recordStats()
                .build());
    }
}
