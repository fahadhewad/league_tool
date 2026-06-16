package gg.leaguetool.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import gg.leaguetool.common.Platform;
import gg.leaguetool.common.Region;
import gg.leaguetool.profile.model.PlayerProfile;
import gg.leaguetool.riot.RiotApiClient;
import gg.leaguetool.riot.dto.AccountDto;
import gg.leaguetool.riot.dto.ChampionMasteryDto;
import gg.leaguetool.riot.dto.LeagueEntryDto;
import gg.leaguetool.riot.dto.MatchDto;
import gg.leaguetool.riot.dto.SummonerDto;
import gg.leaguetool.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private static final String PUUID = "PUUID-AGURIN-0001";

    private RiotApiClient riot;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        riot = mock(RiotApiClient.class);
        var champions = new gg.leaguetool.champion.ChampionRepository(new com.fasterxml.jackson.databind.ObjectMapper());
        service = new ProfileService(riot, new MatchHistoryService(riot), champions);

        AccountDto account = Fixtures.read("account.json", AccountDto.class);
        SummonerDto summoner = Fixtures.read("summoner.json", SummonerDto.class);
        List<LeagueEntryDto> league = Fixtures.read("league.json", new TypeReference<>() {});
        List<ChampionMasteryDto> mastery = Fixtures.read("mastery.json", new TypeReference<>() {});
        List<String> ids = Fixtures.read("match-ids.json", new TypeReference<>() {});
        MatchDto match = Fixtures.read("match.json", MatchDto.class);

        // Second match deliberately has no participant matching our PUUID → should be filtered out.
        MatchDto otherMatch = new MatchDto(
                new MatchDto.Metadata("2", "EUW1_6543210002", List.of("OTHER")),
                new MatchDto.Info(0, 1000, 0, 0, 430, "14.23.1", "CLASSIC", "MATCHED_GAME", List.of()));

        when(riot.getAccountByRiotId(Region.EUROPE, "Agurin", "EUW")).thenReturn(account);
        when(riot.getSummonerByPuuid(Platform.EUW1, PUUID)).thenReturn(summoner);
        when(riot.getLeagueEntriesByPuuid(Platform.EUW1, PUUID)).thenReturn(league);
        when(riot.getChampionMasteries(Platform.EUW1, PUUID)).thenReturn(mastery);
        when(riot.getMatchIds(eq(Region.EUROPE), eq(PUUID), eq(0), eq(20)))
                .thenReturn(ids);
        when(riot.getMatch(Region.EUROPE, "EUW1_6543210001")).thenReturn(match);
        when(riot.getMatch(Region.EUROPE, "EUW1_6543210002")).thenReturn(otherMatch);
    }

    @Test
    void assemblesProfileWithRankedSortedSoloFirst() {
        PlayerProfile profile = service.getProfile(Platform.EUW1, "Agurin", "EUW");

        assertThat(profile.puuid()).isEqualTo(PUUID);
        assertThat(profile.platform()).isEqualTo("euw1");
        assertThat(profile.summonerLevel()).isEqualTo(712);
        assertThat(profile.ranked().get(0).queueType()).isEqualTo("RANKED_SOLO_5x5");
        // 320 / (320 + 250) = 56.1%
        assertThat(profile.ranked().get(0).winRate()).isEqualTo(56.1);
    }

    @Test
    void topMasteryIsSortedDescendingAndCapped() {
        PlayerProfile profile = service.getProfile(Platform.EUW1, "Agurin", "EUW");

        assertThat(profile.topMastery()).hasSize(5);
        assertThat(profile.topMastery().get(0).championId()).isEqualTo(121); // 523k points, highest
        assertThat(profile.topMastery().get(0).championPoints()).isEqualTo(523000);
    }

    @Test
    void recentMatchesFilterOutGamesWherePlayerIsAbsent() {
        PlayerProfile profile = service.getProfile(Platform.EUW1, "Agurin", "EUW");

        assertThat(profile.recentMatches()).hasSize(1);
        var m = profile.recentMatches().get(0);
        assertThat(m.championName()).isEqualTo("LeeSin");
        assertThat(m.win()).isTrue();
        assertThat(m.kda()).isEqualTo(7.33); // (8 + 14) / 3
        assertThat(m.csTotal()).isEqualTo(210); // 42 + 168
    }
}
