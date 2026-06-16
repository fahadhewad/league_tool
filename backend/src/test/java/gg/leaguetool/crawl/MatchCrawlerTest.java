package gg.leaguetool.crawl;

import gg.leaguetool.common.Platform;
import gg.leaguetool.common.Region;
import gg.leaguetool.riot.RiotApiClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchCrawlerTest {

    private final RiotApiClient riot = mock(RiotApiClient.class);
    private final MatchIngestService ingest = mock(MatchIngestService.class);
    private final MatchCrawler crawler = new MatchCrawler(riot, ingest);

    @Test
    void crawlsUpToTheMatchLimitAndStops() {
        when(riot.getMatchIds(Region.EUROPE, "seed", 0, 20)).thenReturn(List.of("M1", "M2", "M3"));
        when(riot.getMatch(Region.EUROPE, "M1")).thenReturn(MatchFixtures.standardMatch("M1", 1, true));
        when(riot.getMatch(Region.EUROPE, "M2")).thenReturn(MatchFixtures.standardMatch("M2", 11, true));
        when(ingest.ingest(any())).thenReturn(true);

        int ingested = crawler.crawl(Platform.EUW1, "seed", 2);

        assertThat(ingested).isEqualTo(2);
        verify(ingest, times(2)).ingest(any());
        verify(riot).getMatch(Region.EUROPE, "M1");
        verify(riot).getMatch(Region.EUROPE, "M2");
        verify(riot, never()).getMatch(Region.EUROPE, "M3"); // stopped at the limit
    }
}
