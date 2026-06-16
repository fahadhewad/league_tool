package gg.leaguetool.profile;

import gg.leaguetool.common.Platform;
import gg.leaguetool.profile.model.MatchSummary;
import gg.leaguetool.riot.RiotApiClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Fetches a player's recent matches as {@link MatchSummary} rows (most-recent first), reused by both
 * the profile and recent-form features. Each underlying Riot call is cached and rate-limited.
 */
@Service
public class MatchHistoryService {

    private final RiotApiClient riot;

    public MatchHistoryService(RiotApiClient riot) {
        this.riot = riot;
    }

    public List<MatchSummary> recentMatches(Platform platform, String puuid, int count) {
        List<String> ids = riot.getMatchIds(platform.matchRegion(), puuid, 0, count);
        return ids.stream()
                .map(id -> riot.getMatch(platform.matchRegion(), id))
                .map(match -> MatchSummary.forPlayer(match, puuid))
                .filter(Objects::nonNull)
                .toList();
    }
}
