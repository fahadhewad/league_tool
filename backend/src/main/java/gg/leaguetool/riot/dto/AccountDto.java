package gg.leaguetool.riot.dto;

/** Account-V1 response: the stable identity behind a Riot ID. */
public record AccountDto(String puuid, String gameName, String tagLine) {
}
