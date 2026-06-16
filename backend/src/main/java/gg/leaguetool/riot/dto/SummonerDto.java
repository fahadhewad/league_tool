package gg.leaguetool.riot.dto;

/**
 * Summoner-V4 response. Note Riot no longer returns the summoner name here; identity comes from
 * Account-V1 instead. {@code id}/{@code accountId} may be absent on newer responses.
 */
public record SummonerDto(
        String id,
        String accountId,
        String puuid,
        int profileIconId,
        long revisionDate,
        long summonerLevel) {
}
