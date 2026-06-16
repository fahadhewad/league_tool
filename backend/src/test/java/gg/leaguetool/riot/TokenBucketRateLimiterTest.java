package gg.leaguetool.riot;

import gg.leaguetool.config.RiotApiProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketRateLimiterTest {

    private static RiotApiProperties props(int perSecond, int perTwoMin) {
        return new RiotApiProperties("k", "euw1", "https://%s.api.riotgames.com", 8000,
                new RiotApiProperties.RateLimit(perSecond, perTwoMin),
                new RiotApiProperties.Retry(3, 1000, 20000));
    }

    @Test
    void allowsRequestsUpToTheSecondLimitWithoutBlocking() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props(5, 1000));

        long start = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            limiter.acquire();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(200);
    }

    @Test
    void blocksWhenTheSecondLimitIsExceeded() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props(5, 1000));

        for (int i = 0; i < 5; i++) {
            limiter.acquire();
        }

        long start = System.nanoTime();
        limiter.acquire(); // 6th within the same second must wait for the window to roll
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isGreaterThan(700);
    }
}
