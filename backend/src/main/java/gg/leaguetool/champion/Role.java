package gg.leaguetool.champion;

import java.util.Locale;

/** The five Summoner's Rift positions, aligned with Riot's {@code teamPosition} values. */
public enum Role {
    TOP,
    JUNGLE,
    MIDDLE,
    BOTTOM,
    UTILITY;

    /** Maps a Riot {@code teamPosition} string to a role, or {@code null} if unknown/empty. */
    public static Role fromTeamPosition(String teamPosition) {
        if (teamPosition == null || teamPosition.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(teamPosition.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
