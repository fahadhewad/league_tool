package gg.leaguetool.champion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operator endpoint to refresh the champion roster from Data Dragon without a restart. Protected by
 * the admin token filter (it lives under {@code /api/v1/admin}).
 */
@RestController
@RequestMapping("/api/v1/admin/champions")
@Tag(name = "Admin", description = "Operational endpoints (crawler, static-data refresh)")
public class AdminChampionController {

    private final DataDragonService dataDragon;

    public AdminChampionController(DataDragonService dataDragon) {
        this.dataDragon = dataDragon;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh the champion roster from Data Dragon")
    public RefreshResult refresh() {
        int count = dataDragon.refresh();
        return new RefreshResult(dataDragon.currentVersion(), count);
    }

    public record RefreshResult(String version, int champions) {
    }
}
