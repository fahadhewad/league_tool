package gg.leaguetool.config;

import gg.leaguetool.riot.RateLimiter;
import gg.leaguetool.riot.RedisRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With {@code app.redis.enabled=true} the context wires the Redis-backed limiter and cache manager
 * (and starts without a live Redis, since both connect lazily).
 */
@SpringBootTest(properties = "app.redis.enabled=true")
class RedisWiringTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void usesRedisBackedInfrastructure() {
        assertThat(rateLimiter).isInstanceOf(RedisRateLimiter.class);
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }
}
