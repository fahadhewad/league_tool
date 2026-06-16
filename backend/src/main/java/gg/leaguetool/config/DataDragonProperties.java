package gg.leaguetool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the Data Dragon static-data client, bound from {@code champion.datadragon.*}.
 *
 * <p>Data Dragon is Riot's public static-data CDN (no API key required). It is the source of the
 * full champion roster (id, name, class tags); positions and crowd-control come from the curated
 * {@code champion-overrides.json}.
 *
 * @param baseUrl          Data Dragon base URL (overridden by tests to point at a stub)
 * @param locale           locale for champion names, e.g. {@code en_US}
 * @param refreshOnStartup when true, refresh the in-memory roster from Data Dragon at boot
 *                         (best-effort; falls back to the bundled seed on any failure)
 * @param timeoutMs        per-request connect/read timeout in milliseconds
 */
@ConfigurationProperties(prefix = "champion.datadragon")
public record DataDragonProperties(
        @DefaultValue("https://ddragon.leagueoflegends.com") String baseUrl,
        @DefaultValue("en_US") String locale,
        @DefaultValue("false") boolean refreshOnStartup,
        @DefaultValue("8000") int timeoutMs) {
}
