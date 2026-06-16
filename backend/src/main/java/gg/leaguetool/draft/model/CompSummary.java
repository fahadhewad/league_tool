package gg.leaguetool.draft.model;

import java.util.List;

/**
 * A team-composition snapshot: damage split, frontline/CC presence, and human-readable notes.
 *
 * @param championCount  champions resolved into the summary
 * @param physicalCount  champions whose primary damage is physical
 * @param magicCount     champions whose primary damage is magic
 * @param mixedCount     champions with mixed damage
 * @param frontlineCount champions that can hold a front line
 * @param ccCount        champions with reliable hard CC
 * @param carryCount     primary damage carries
 * @param damageBalance  one of "AD-heavy", "AP-heavy", "Balanced"
 * @param notes          coverage observations
 */
public record CompSummary(
        int championCount,
        int physicalCount,
        int magicCount,
        int mixedCount,
        int frontlineCount,
        int ccCount,
        int carryCount,
        String damageBalance,
        List<String> notes) {
}
