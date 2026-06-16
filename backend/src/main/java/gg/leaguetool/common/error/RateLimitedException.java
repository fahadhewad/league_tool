package gg.leaguetool.common.error;

/** Thrown when the Riot API returns 429 and retries are exhausted. */
public class RateLimitedException extends RuntimeException {
    public RateLimitedException(String message) {
        super(message);
    }
}
