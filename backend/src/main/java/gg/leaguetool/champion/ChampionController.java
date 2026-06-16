package gg.leaguetool.champion;

import gg.leaguetool.common.error.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Static champion metadata used by the draft tools and clients. */
@RestController
@RequestMapping("/api/v1/champions")
@Tag(name = "Champions", description = "Champion metadata (roles, damage type, tags)")
public class ChampionController {

    private final ChampionRepository champions;

    public ChampionController(ChampionRepository champions) {
        this.champions = champions;
    }

    @GetMapping
    @Operation(summary = "List champions, optionally filtered by role")
    public List<Champion> list(@RequestParam(required = false) Role role) {
        return role == null ? champions.all() : champions.byRole(role);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a champion by id")
    public Champion byId(@PathVariable int id) {
        return champions.byId(id)
                .orElseThrow(() -> new ResourceNotFoundException("champion " + id));
    }
}
