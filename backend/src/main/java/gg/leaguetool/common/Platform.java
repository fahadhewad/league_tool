package gg.leaguetool.common;

import java.util.Locale;

/**
 * Riot platform routing values (e.g. {@code euw1}) and their mapping to regional routes.
 *
 * <p>Account-V1 only accepts {@code americas}/{@code asia}/{@code europe}, while Match-V5 also
 * accepts {@code sea}. Each platform therefore exposes both an {@link #accountRegion()} and a
 * {@link #matchRegion()}; for most platforms these are the same.
 */
public enum Platform {
    NA1(Region.AMERICAS, Region.AMERICAS),
    BR1(Region.AMERICAS, Region.AMERICAS),
    LA1(Region.AMERICAS, Region.AMERICAS),
    LA2(Region.AMERICAS, Region.AMERICAS),
    OC1(Region.AMERICAS, Region.SEA),
    KR(Region.ASIA, Region.ASIA),
    JP1(Region.ASIA, Region.ASIA),
    EUW1(Region.EUROPE, Region.EUROPE),
    EUN1(Region.EUROPE, Region.EUROPE),
    TR1(Region.EUROPE, Region.EUROPE),
    RU(Region.EUROPE, Region.EUROPE),
    PH2(Region.ASIA, Region.SEA),
    SG2(Region.ASIA, Region.SEA),
    TH2(Region.ASIA, Region.SEA),
    TW2(Region.ASIA, Region.SEA),
    VN2(Region.ASIA, Region.SEA);

    private final Region accountRegion;
    private final Region matchRegion;

    Platform(Region accountRegion, Region matchRegion) {
        this.accountRegion = accountRegion;
        this.matchRegion = matchRegion;
    }

    /** Regional route for Account-V1 (Riot ID ↔ PUUID). */
    public Region accountRegion() {
        return accountRegion;
    }

    /** Regional route for Match-V5 (history + match details). */
    public Region matchRegion() {
        return matchRegion;
    }

    /** Lower-case host segment, e.g. {@code euw1} in {@code euw1.api.riotgames.com}. */
    public String host() {
        return name().toLowerCase();
    }

    /** Case-insensitive lookup that throws a clear error for unknown platforms. */
    public static Platform fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Platform must not be blank");
        }
        try {
            return Platform.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown platform '" + value + "'. Valid values: "
                    + java.util.Arrays.toString(values()));
        }
    }
}
