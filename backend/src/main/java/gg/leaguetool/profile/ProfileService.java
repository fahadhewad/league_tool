package gg.leaguetool.profile;

import gg.leaguetool.common.Platform;
import gg.leaguetool.profile.model.ChampionMasterySummary;
import gg.leaguetool.profile.model.MatchSummary;
import gg.leaguetool.profile.model.PlayerProfile;
import gg.leaguetool.profile.model.RankedStats;
import gg.leaguetool.riot.RiotApiClient;
import gg.leaguetool.riot.dto.AccountDto;
import gg.leaguetool.riot.dto.SummonerDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link PlayerProfile} by composing Account-V1, Summoner-V4, League-V4,
 * Champion-Mastery-V4 and Match-V5 calls. All underlying calls are individually cached and
 * rate-limited by {@link RiotApiClient}.
 */
@Service
public class ProfileService {

    public static final int DEFAULT_MATCH_COUNT = 20;
    public static final int MAX_MATCH_COUNT = 20;
    private static final int TOP_MASTERY = 5;

    /** Ranked queue ordering: Solo/Duo first, then Flex, then anything else. */
    private static final Comparator<RankedStats> QUEUE_ORDER = Comparator.comparingInt(r ->
            switch (r.queueType() == null ? "" : r.queueType()) {
                case "RANKED_SOLO_5x5" -> 0;
                case "RANKED_FLEX_SR" -> 1;
                default -> 2;
            });

    private final RiotApiClient riot;

    public ProfileService(RiotApiClient riot) {
        this.riot = riot;
    }

    public PlayerProfile getProfile(Platform platform, String gameName, String tagLine) {
        return getProfile(platform, gameName, tagLine, DEFAULT_MATCH_COUNT);
    }

    public PlayerProfile getProfile(Platform platform, String gameName, String tagLine, int matchCount) {
        int count = Math.clamp(matchCount, 1, MAX_MATCH_COUNT);

        AccountDto account = riot.getAccountByRiotId(platform.accountRegion(), gameName, tagLine);
        String puuid = account.puuid();
        SummonerDto summoner = riot.getSummonerByPuuid(platform, puuid);

        List<RankedStats> ranked = riot.getLeagueEntriesByPuuid(platform, puuid).stream()
                .map(RankedStats::from)
                .sorted(QUEUE_ORDER)
                .toList();

        List<ChampionMasterySummary> topMastery = riot.getChampionMasteries(platform, puuid).stream()
                .sorted(Comparator.comparingLong(gg.leaguetool.riot.dto.ChampionMasteryDto::championPoints).reversed())
                .limit(TOP_MASTERY)
                .map(m -> ChampionMasterySummary.from(m, null))
                .toList();

        List<String> matchIds = riot.getMatchIds(platform.matchRegion(), puuid, 0, count);
        List<MatchSummary> recentMatches = matchIds.stream()
                .map(id -> riot.getMatch(platform.matchRegion(), id))
                .map(match -> MatchSummary.forPlayer(match, puuid))
                .filter(Objects::nonNull)
                .toList();

        return new PlayerProfile(
                puuid,
                account.gameName(),
                account.tagLine(),
                platform.host(),
                summoner.summonerLevel(),
                summoner.profileIconId(),
                ranked,
                topMastery,
                recentMatches);
    }
}
