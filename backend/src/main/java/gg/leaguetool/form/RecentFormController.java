package gg.leaguetool.form;

import gg.leaguetool.common.Platform;
import gg.leaguetool.form.model.RecentForm;
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
 * Recent-form / tilt score for a player you legitimately have the Riot ID for (your party, or a
 * manual search). Example: {@code GET /api/v1/form/euw1/Agurin/EUW?games=10}.
 */
@RestController
@RequestMapping("/api/v1/form")
@Tag(name = "Recent form", description = "Last-N-games performance and tilt assessment")
public class RecentFormController {

    private final RecentFormService recentFormService;

    public RecentFormController(RecentFormService recentFormService) {
        this.recentFormService = recentFormService;
    }

    @GetMapping("/{platform}/{gameName}/{tagLine}")
    @Operation(summary = "Compute recent-form / tilt score from the last N games")
    public RecentForm getRecentForm(
            @PathVariable String platform,
            @PathVariable String gameName,
            @PathVariable String tagLine,
            @RequestParam(defaultValue = "" + RecentFormService.DEFAULT_GAMES)
            @Min(RecentFormService.MIN_GAMES) @Max(RecentFormService.MAX_GAMES) int games) {
        Platform p = Platform.fromString(platform);
        return recentFormService.analyze(p, gameName, tagLine, games);
    }
}
