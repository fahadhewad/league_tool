package gg.leaguetool.draft.model;

/**
 * A notable matchup where an enemy is favoured into one of your champions.
 *
 * @param allyChampionId  your champion
 * @param allyName        your champion's name
 * @param enemyChampionId the enemy favoured into them
 * @param enemyName       the enemy's name
 * @param disadvantage    your disadvantage in percentage points (positive number = how unfavoured)
 * @param note            short explanation
 */
public record CounterNote(
        int allyChampionId,
        String allyName,
        int enemyChampionId,
        String enemyName,
        double disadvantage,
        String note) {
}
