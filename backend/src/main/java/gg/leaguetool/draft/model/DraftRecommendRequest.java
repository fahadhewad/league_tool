package gg.leaguetool.draft.model;

import gg.leaguetool.champion.Role;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request for a champion pick recommendation: given allies and enemies already picked (plus bans),
 * which champion should you pick for your open {@code role}?
 */
public record DraftRecommendRequest(
        @NotNull Role role,
        List<DraftPick> allies,
        List<DraftPick> enemies,
        List<Integer> bans) {

    public List<DraftPick> alliesOrEmpty() {
        return allies == null ? List.of() : allies;
    }

    public List<DraftPick> enemiesOrEmpty() {
        return enemies == null ? List.of() : enemies;
    }

    public List<Integer> bansOrEmpty() {
        return bans == null ? List.of() : bans;
    }
}
