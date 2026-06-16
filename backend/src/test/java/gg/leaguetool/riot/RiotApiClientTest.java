package gg.leaguetool.riot;

import com.github.tomakehurst.wiremock.WireMockServer;
import gg.leaguetool.common.Platform;
import gg.leaguetool.common.Region;
import gg.leaguetool.common.error.RateLimitedException;
import gg.leaguetool.common.error.ResourceNotFoundException;
import gg.leaguetool.config.RiotApiProperties;
import gg.leaguetool.riot.dto.AccountDto;
import gg.leaguetool.riot.dto.MatchDto;
import gg.leaguetool.support.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiotApiClientTest {

    private WireMockServer server;
    private RiotApiClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();

        RiotApiProperties props = new RiotApiProperties(
                "test-key",
                "euw1",
                "http://localhost:" + server.port(), // no %s → host is fixed to WireMock
                8000,
                new RiotApiProperties.RateLimit(1000, 1000),
                new RiotApiProperties.Retry(3, 10, 50));

        RestClient restClient = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("X-Riot-Token", props.key());
                    return execution.execute(request, body);
                })
                .build();

        client = new RiotApiClient(restClient, new TokenBucketRateLimiter(props), props);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void resolvesAccountByRiotId() {
        server.stubFor(get(urlPathEqualTo("/riot/account/v1/accounts/by-riot-id/Agurin/EUW"))
                .willReturn(okJson(Fixtures.load("account.json"))));

        AccountDto account = client.getAccountByRiotId(Region.EUROPE, "Agurin", "EUW");

        assertThat(account.puuid()).isEqualTo("PUUID-AGURIN-0001");
        assertThat(account.gameName()).isEqualTo("Agurin");
    }

    @Test
    void urlEncodesNamesWithSpaces() {
        server.stubFor(get(urlEqualTo("/riot/account/v1/accounts/by-riot-id/Hide%20on%20bush/KR1"))
                .willReturn(okJson("{\"puuid\":\"P\",\"gameName\":\"Hide on bush\",\"tagLine\":\"KR1\"}")));

        AccountDto account = client.getAccountByRiotId(Region.ASIA, "Hide on bush", "KR1");

        assertThat(account.gameName()).isEqualTo("Hide on bush");
    }

    @Test
    void parsesLeagueEntries() {
        server.stubFor(get(urlPathEqualTo(
                "/lol/league/v4/entries/by-puuid/PUUID-AGURIN-0001"))
                .willReturn(okJson(Fixtures.load("league.json"))));

        var entries = client.getLeagueEntriesByPuuid(Platform.EUW1, "PUUID-AGURIN-0001");

        assertThat(entries).hasSize(2);
        assertThat(entries).anyMatch(e -> "RANKED_SOLO_5x5".equals(e.queueType()) && e.wins() == 320);
    }

    @Test
    void parsesMatchDetails() {
        server.stubFor(get(urlPathEqualTo("/lol/match/v5/matches/EUW1_6543210001"))
                .willReturn(okJson(Fixtures.load("match.json"))));

        MatchDto match = client.getMatch(Region.EUROPE, "EUW1_6543210001");

        assertThat(match.metadata().matchId()).isEqualTo("EUW1_6543210001");
        assertThat(match.info().participants()).hasSize(2);
        assertThat(match.info().queueId()).isEqualTo(420);
    }

    @Test
    void sendsMatchIdsQueryParams() {
        server.stubFor(get(urlPathEqualTo("/lol/match/v5/matches/by-puuid/PUUID-AGURIN-0001/ids"))
                .willReturn(okJson(Fixtures.load("match-ids.json"))));

        var ids = client.getMatchIds(Region.EUROPE, "PUUID-AGURIN-0001", 0, 20);

        assertThat(ids).containsExactly("EUW1_6543210001", "EUW1_6543210002");
        server.verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
                        urlPathEqualTo("/lol/match/v5/matches/by-puuid/PUUID-AGURIN-0001/ids"))
                .withQueryParam("start", com.github.tomakehurst.wiremock.client.WireMock.equalTo("0"))
                .withQueryParam("count", com.github.tomakehurst.wiremock.client.WireMock.equalTo("20")));
    }

    @Test
    void maps404ToResourceNotFound() {
        server.stubFor(get(urlPathEqualTo("/lol/summoner/v4/summoners/by-puuid/missing"))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.getSummonerByPuuid(Platform.EUW1, "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void retriesThenThrowsRateLimitedOn429() {
        server.stubFor(get(urlPathEqualTo("/lol/summoner/v4/summoners/by-puuid/limited"))
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0")));

        assertThatThrownBy(() -> client.getSummonerByPuuid(Platform.EUW1, "limited"))
                .isInstanceOf(RateLimitedException.class);

        // 3 attempts configured → 3 upstream calls.
        server.verify(3, com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
                urlPathEqualTo("/lol/summoner/v4/summoners/by-puuid/limited")));
    }
}
