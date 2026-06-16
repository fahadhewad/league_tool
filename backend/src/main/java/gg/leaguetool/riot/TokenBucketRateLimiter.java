package gg.leaguetool.riot;

import gg.leaguetool.config.RiotApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Single-instance sliding-window rate limiter that enforces both the per-second and the
 * per-2-minute Riot budgets simultaneously. {@link #acquire()} blocks until both windows allow a
 * request, so callers never have to reason about limits themselves.
 *
 * <p>This is the default. For multi-instance deployments set {@code app.redis.enabled=true} to use
 * {@link RedisRateLimiter}, which shares the budget across instances behind this same interface.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class TokenBucketRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final SlidingWindow perSecond;
    private final SlidingWindow perTwoMinutes;
    private final Object lock = new Object();

    public TokenBucketRateLimiter(RiotApiProperties props) {
        this.perSecond = new SlidingWindow(props.rateLimit().requestsPerSecond(), 1_000L);
        this.perTwoMinutes = new SlidingWindow(props.rateLimit().requestsPerTwoMinutes(), 120_000L);
    }

    @Override
    public void acquire() {
        while (true) {
            long waitMs;
            synchronized (lock) {
                long now = System.currentTimeMillis();
                long w = Math.max(perSecond.waitTime(now), perTwoMinutes.waitTime(now));
                if (w <= 0) {
                    perSecond.record(now);
                    perTwoMinutes.record(now);
                    return;
                }
                waitMs = w;
            }
            if (log.isTraceEnabled()) {
                log.trace("Rate limit reached; sleeping {}ms", waitMs);
            }
            sleep(waitMs);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(Math.max(1, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for rate-limit token", e);
        }
    }

    /** A fixed-capacity sliding window over a time span. Not thread-safe; guarded by the outer lock. */
    private static final class SlidingWindow {
        private final int limit;
        private final long windowMs;
        private final Deque<Long> timestamps = new ArrayDeque<>();

        SlidingWindow(int limit, long windowMs) {
            this.limit = limit;
            this.windowMs = windowMs;
        }

        /** Milliseconds to wait before a request is allowed; 0 if allowed now. */
        long waitTime(long now) {
            evict(now);
            if (timestamps.size() < limit) {
                return 0L;
            }
            long oldest = timestamps.peekFirst();
            return (oldest + windowMs) - now;
        }

        void record(long now) {
            timestamps.addLast(now);
        }

        private void evict(long now) {
            long cutoff = now - windowMs;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.pollFirst();
            }
        }
    }
}
