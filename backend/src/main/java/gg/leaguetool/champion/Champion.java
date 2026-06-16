package gg.leaguetool.champion;

import java.util.List;

/**
 * Static champion metadata used by the draft analyzer and pick recommender.
 *
 * @param id         Riot champion id (e.g. 64 = Lee Sin)
 * @param name       display name
 * @param roles      positions this champion is commonly played in
 * @param damageType primary damage profile
 * @param tags       Riot class tags (Fighter, Mage, …)
 * @param cc         whether the champion brings reliable hard crowd control
 */
public record Champion(
        int id,
        String name,
        List<Role> roles,
        DamageType damageType,
        List<ChampionTag> tags,
        boolean cc) {

    public boolean playsRole(Role role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasTag(ChampionTag tag) {
        return tags != null && tags.contains(tag);
    }

    /** A champion that can hold a front line (tank or fighter). */
    public boolean isFrontline() {
        return hasTag(ChampionTag.TANK) || hasTag(ChampionTag.FIGHTER);
    }

    /** A primarily damage-dealing carry profile. */
    public boolean isCarry() {
        return hasTag(ChampionTag.MARKSMAN) || hasTag(ChampionTag.MAGE) || hasTag(ChampionTag.ASSASSIN);
    }
}
