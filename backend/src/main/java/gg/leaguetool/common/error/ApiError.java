package gg.leaguetool.common.error;

import java.time.Instant;

/**
 * Standard error body returned by the API.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     short reason phrase
 * @param message   human-readable detail
 * @param path      request path that produced the error
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path);
    }
}
