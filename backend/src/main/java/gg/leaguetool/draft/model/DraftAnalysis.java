package gg.leaguetool.draft.model;

import java.util.List;

/**
 * Result of analyzing a champ-select state.
 *
 * @param allyComp          your team's composition snapshot
 * @param enemyComp         the enemy composition snapshot
 * @param allySynergyScore  summed pairwise synergy among your champions (percentage points)
 * @param synergyNotes      the strongest synergy pairings, explained
 * @param threats           matchups where an enemy is favoured into one of your champions
 * @param recommendations   actionable suggestions to round out your comp
 */
public record DraftAnalysis(
        CompSummary allyComp,
        CompSummary enemyComp,
        double allySynergyScore,
        List<String> synergyNotes,
        List<CounterNote> threats,
        List<String> recommendations) {
}
