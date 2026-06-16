package gg.leaguetool.profile;

import gg.leaguetool.common.Platform;
import gg.leaguetool.profile.model.PlayerProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Player profile lookup by Riot ID.
 *
 * <p>Example: {@code GET /api/v1/profiles/euw1/Agurin/EUW}. The {@code gameName} and {@code tagLine}
 * segments should be URL-encoded by the client (names can contain spaces).
 */
@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Profiles", description = "Riot ID → ranked stats, recent matches, and champion mastery")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{platform}/{gameName}/{tagLine}")
    @Operation(summary = "Look up a player profile by Riot ID")
    public PlayerProfile getProfile(
            @PathVariable String platform,
            @PathVariable String gameName,
            @PathVariable String tagLine,
            @RequestParam(defaultValue = "" + ProfileService.DEFAULT_MATCH_COUNT)
            @Min(1) @Max(ProfileService.MAX_MATCH_COUNT) int matchCount) {
        Platform p = Platform.fromString(platform);
        return profileService.getProfile(p, gameName, tagLine, matchCount);
    }
}
