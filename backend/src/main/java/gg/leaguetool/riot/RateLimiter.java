package gg.leaguetool.riot;

/** Blocks the caller until a request is permitted under the configured budget. */
public interface RateLimiter {

    /** Acquire permission to make one request, blocking (sleeping) if necessary. */
    void acquire();
}
