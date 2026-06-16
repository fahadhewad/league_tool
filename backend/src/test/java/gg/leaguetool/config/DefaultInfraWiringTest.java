package gg.leaguetool.config;

import gg.leaguetool.riot.RateLimiter;
import gg.leaguetool.riot.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/** By default (no Redis) the context uses the in-process limiter and Caffeine cache manager. */
@SpringBootTest
class DefaultInfraWiringTest {

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void usesInProcessInfrastructure() {
        assertThat(rateLimiter).isInstanceOf(TokenBucketRateLimiter.class);
        assertThat(cacheManager).isInstanceOf(SimpleCacheManager.class);
    }
}
