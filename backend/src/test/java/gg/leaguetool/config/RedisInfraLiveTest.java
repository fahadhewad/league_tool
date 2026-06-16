package gg.leaguetool.config;

import gg.leaguetool.riot.RedisRateLimiter;
import gg.leaguetool.riot.dto.AccountDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the Redis-backed limiter and cache against a real Redis. Skips automatically when no
 * Redis is reachable on localhost:6379 (e.g. in CI), so it never breaks the build; run a local
 * {@code redis-server} to execute it.
 */
class RedisInfraLiveTest {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        assumeTrue(reachable(connectionFactory), "Redis not reachable on localhost:6379 - skipping");
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
        redis.delete("leaguetool:ratelimit:riot:1s");
        redis.delete("leaguetool:ratelimit:riot:2m");
    }

    @Test
    void limiterThrottlesAcrossTheConfiguredBudget() {
        // 5 requests/second: ten acquires must therefore span at least ~1 second of throttling.
        RiotApiProperties props = new RiotApiProperties(
                "k", "euw1", "https://%s.api.riotgames.com", 8000,
                new RiotApiProperties.RateLimit(5, 100), new RiotApiProperties.Retry(1, 1, 1));
        RedisRateLimiter limiter = new RedisRateLimiter(redis, props);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            limiter.acquire();
        }
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).as("10 acquires at 5/s should be throttled to ~1s+").isGreaterThanOrEqualTo(800L);
        assertThat(elapsed).as("but must not hang").isLessThan(5_000L);
    }

    @Test
    void cacheRoundTripsRecordValues() {
        RedisCacheManager manager = new RedisConfig().cacheManager(connectionFactory);
        Cache cache = manager.getCache(CacheConfig.ACCOUNT);
        assertThat(cache).isNotNull();

        AccountDto value = new AccountDto("PUUID-1", "Agurin", "EUW");
        cache.put("acct-key", value);

        AccountDto back = cache.get("acct-key", AccountDto.class);
        assertThat(back).isEqualTo(value);
    }

    private static boolean reachable(RedisConnectionFactory factory) {
        try {
            factory.getConnection().ping();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
