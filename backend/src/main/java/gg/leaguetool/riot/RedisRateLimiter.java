package gg.leaguetool.riot;

import gg.leaguetool.config.RiotApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Distributed rate limiter that shares the Riot budget across backend instances via Redis, so the
 * global per-second and per-2-minute limits hold no matter how many instances run.
 *
 * <p>A single Lua script atomically evicts expired entries from two sliding-window sorted sets,
 * checks both budgets, and either records the request (returning 0) or returns the milliseconds to
 * wait. {@link #acquire()} loops on that, sleeping as instructed, so it behaves exactly like the
 * in-process {@link TokenBucketRateLimiter} from a caller's point of view.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final String KEY_SECOND = "leaguetool:ratelimit:riot:1s";
    private static final String KEY_TWO_MIN = "leaguetool:ratelimit:riot:2m";
    private static final long SECOND_MS = 1_000L;
    private static final long TWO_MIN_MS = 120_000L;

    /**
     * KEYS: [1] per-second zset, [2] per-2-minute zset.
     * ARGV: now, secWindowMs, secLimit, minWindowMs, minLimit, member.
     * Returns 0 when the request is recorded, else the milliseconds to wait before retrying.
     */
    private static final String SCRIPT = """
            local now = tonumber(ARGV[1])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - tonumber(ARGV[2]))
            redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, now - tonumber(ARGV[4]))
            local cs = redis.call('ZCARD', KEYS[1])
            local cm = redis.call('ZCARD', KEYS[2])
            if cs < tonumber(ARGV[3]) and cm < tonumber(ARGV[5]) then
              redis.call('ZADD', KEYS[1], now, ARGV[6])
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
              redis.call('ZADD', KEYS[2], now, ARGV[6])
              redis.call('PEXPIRE', KEYS[2], ARGV[4])
              return 0
            end
            local wait = 1
            if cs >= tonumber(ARGV[3]) then
              local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
              wait = math.max(wait, (tonumber(oldest[2]) + tonumber(ARGV[2])) - now)
            end
            if cm >= tonumber(ARGV[5]) then
              local oldest = redis.call('ZRANGE', KEYS[2], 0, 0, 'WITHSCORES')
              wait = math.max(wait, (tonumber(oldest[2]) + tonumber(ARGV[4])) - now)
            end
            return wait
            """;

    private final StringRedisTemplate redis;
    private final RedisScript<Long> script;
    private final int perSecond;
    private final int perTwoMinutes;

    public RedisRateLimiter(StringRedisTemplate redis, RiotApiProperties props) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(SCRIPT, Long.class);
        this.perSecond = props.rateLimit().requestsPerSecond();
        this.perTwoMinutes = props.rateLimit().requestsPerTwoMinutes();
    }

    @Override
    public void acquire() {
        List<String> keys = List.of(KEY_SECOND, KEY_TWO_MIN);
        while (true) {
            long now = System.currentTimeMillis();
            String member = now + "-" + ThreadLocalRandom.current().nextLong();
            Long waitMs = redis.execute(script, keys,
                    Long.toString(now),
                    Long.toString(SECOND_MS), Integer.toString(perSecond),
                    Long.toString(TWO_MIN_MS), Integer.toString(perTwoMinutes),
                    member);
            if (waitMs == null || waitMs <= 0) {
                return;
            }
            if (log.isTraceEnabled()) {
                log.trace("Distributed rate limit reached; sleeping {}ms", waitMs);
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
}
