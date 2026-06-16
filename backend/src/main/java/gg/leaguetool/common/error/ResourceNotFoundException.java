package gg.leaguetool.common.error;

/** Thrown when a Riot resource (account, summoner, match, …) returns 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
