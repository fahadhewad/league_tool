package gg.leaguetool.profile.model;

/**
 * Condensed champion-mastery entry. {@code championName} is populated once static data
 * (Data Dragon) is wired in; until then it may be {@code null}.
 */
public record ChampionMasterySummary(
        int championId,
        String championName,
        int championLevel,
        long championPoints) {

    public static ChampionMasterySummary from(gg.leaguetool.riot.dto.ChampionMasteryDto m, String championName) {
        return new ChampionMasterySummary(m.championId(), championName, m.championLevel(), m.championPoints());
    }
}
