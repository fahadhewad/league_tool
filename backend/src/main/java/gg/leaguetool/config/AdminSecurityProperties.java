package gg.leaguetool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for protecting operator endpoints under {@code /api/v1/admin/**}, bound from
 * {@code admin.*}.
 *
 * @param apiToken   shared secret required in the {@code headerName} header. When blank, admin
 *                   endpoints are <strong>disabled</strong> (fail-closed) so an unconfigured
 *                   deployment can never be crawled by an anonymous caller.
 * @param headerName request header carrying the token
 */
@ConfigurationProperties(prefix = "admin")
public record AdminSecurityProperties(
        @DefaultValue("") String apiToken,
        @DefaultValue("X-Admin-Token") String headerName) {

    public boolean enabled() {
        return apiToken != null && !apiToken.isBlank();
    }
}
