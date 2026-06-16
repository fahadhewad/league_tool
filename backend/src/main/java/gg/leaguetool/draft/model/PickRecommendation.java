package gg.leaguetool.draft.model;

import gg.leaguetool.champion.Role;

import java.util.List;

/**
 * A single recommended champion for your open role, with the component scores that produced it.
 *
 * @param championId     recommended champion
 * @param name           champion name
 * @param role           the role this recommendation is for
 * @param score          overall pick score (≈ 50 is neutral; higher is better for this draft)
 * @param synergyScore   average synergy with your allies (percentage points)
 * @param counterScore   average counter advantage vs the enemies (percentage points)
 * @param damageFitScore how well it fills your comp's damage/frontline/CC needs
 * @param reasons        the main human-readable drivers of the score
 */
public record PickRecommendation(
        int championId,
        String name,
        Role role,
        double score,
        double synergyScore,
        double counterScore,
        double damageFitScore,
        List<String> reasons) {
}
