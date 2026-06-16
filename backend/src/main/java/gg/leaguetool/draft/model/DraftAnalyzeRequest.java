package gg.leaguetool.draft.model;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request to analyze a champ-select state from your team's perspective. */
public record DraftAnalyzeRequest(
        @NotNull List<DraftPick> allies,
        @NotNull List<DraftPick> enemies) {

    public List<DraftPick> alliesOrEmpty() {
        return allies == null ? List.of() : allies;
    }

    public List<DraftPick> enemiesOrEmpty() {
        return enemies == null ? List.of() : enemies;
    }
}
