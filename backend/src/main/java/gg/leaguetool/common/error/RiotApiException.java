package gg.leaguetool.common.error;

/** Thrown for non-404/429 errors returned by, or while calling, the Riot API. */
public class RiotApiException extends RuntimeException {
    private final int upstreamStatus;

    public RiotApiException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public RiotApiException(String message, int upstreamStatus, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    public int upstreamStatus() {
        return upstreamStatus;
    }
}
