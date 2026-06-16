package gg.leaguetool.common;

/**
 * Regional routing values for the Riot API.
 *
 * <ul>
 *   <li>{@code AMERICAS}, {@code ASIA}, {@code EUROPE} are valid for Account-V1 and Match-V5.</li>
 *   <li>{@code SEA} is valid for Match-V5 routing only (Account-V1 does not accept it).</li>
 * </ul>
 */
public enum Region {
    AMERICAS,
    ASIA,
    EUROPE,
    SEA;

    /** Lower-case host segment, e.g. {@code europe} in {@code europe.api.riotgames.com}. */
    public String host() {
        return name().toLowerCase();
    }
}
