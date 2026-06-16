package gg.leaguetool.crawl;

import gg.leaguetool.common.Platform;
import gg.leaguetool.common.Region;
import gg.leaguetool.riot.RiotApiClient;
import gg.leaguetool.riot.dto.MatchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the training/aggregate corpus by walking the Match-V5 graph: starting from a seed player,
 * fetch their recent matches, ingest each, and enqueue the other participants to visit next.
 *
 * <p>Bounded by {@code maxMatches}. All Riot calls go through the rate-limited {@link RiotApiClient},
 * so a long crawl self-throttles and never breaches the API limits.
 */
@Service
public class MatchCrawler {

    private static final Logger log = LoggerFactory.getLogger(MatchCrawler.class);
    private static final int PAGE_SIZE = 20;

    private final RiotApiClient riot;
    private final MatchIngestService ingest;

    public MatchCrawler(RiotApiClient riot, MatchIngestService ingest) {
        this.riot = riot;
        this.ingest = ingest;
    }

    /** @return the number of newly ingested matches. */
    public int crawl(Platform platform, String seedPuuid, int maxMatches) {
        Region region = platform.matchRegion();
        Set<String> visitedMatches = new HashSet<>();
        Set<String> visitedPuuids = new HashSet<>();
        Deque<String> frontier = new ArrayDeque<>();
        frontier.add(seedPuuid);
        visitedPuuids.add(seedPuuid);

        int ingested = 0;
        while (!frontier.isEmpty() && ingested < maxMatches) {
            String puuid = frontier.poll();
            List<String> matchIds = riot.getMatchIds(region, puuid, 0, PAGE_SIZE);
            for (String matchId : matchIds) {
                if (ingested >= maxMatches) {
                    break;
                }
                if (!visitedMatches.add(matchId)) {
                    continue;
                }
                MatchDto match = riot.getMatch(region, matchId);
                if (ingest.ingest(match)) {
                    ingested++;
                }
                enqueueParticipants(match, visitedPuuids, frontier);
            }
        }
        log.info("Crawl from {} ingested {} matches", seedPuuid, ingested);
        return ingested;
    }

    private void enqueueParticipants(MatchDto match, Set<String> visitedPuuids, Deque<String> frontier) {
        if (match.info() == null || match.info().participants() == null) {
            return;
        }
        for (MatchDto.Participant p : match.info().participants()) {
            if (p.puuid() != null && visitedPuuids.add(p.puuid())) {
                frontier.add(p.puuid());
            }
        }
    }
}
