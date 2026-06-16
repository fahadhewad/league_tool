package gg.leaguetool.draft;

import gg.leaguetool.champion.Champion;
import gg.leaguetool.champion.ChampionRepository;
import gg.leaguetool.champion.ChampionTag;
import gg.leaguetool.champion.DamageType;
import gg.leaguetool.champion.Role;
import org.springframework.stereotype.Component;

/**
 * Heuristic, attribute-driven {@link DraftDataProvider} used until empirical aggregates from the
 * match crawler are available. Synergy rewards damage diversity and frontline+carry pairings;
 * counters model a simple class rock-paper-scissors (assassins beat squishies, bruisers beat
 * assassins, ranged poke beats immobile tanks, hard CC shuts down assassins).
 *
 * <p>These are explicitly priors, not measured win rates — every value is bounded and explainable.
 */
@Component
public class SeedDraftDataProvider implements DraftDataProvider {

    private final ChampionRepository champions;

    public SeedDraftDataProvider(ChampionRepository champions) {
        this.champions = champions;
    }

    @Override
    public double baseWinRate(int championId, Role role) {
        // Neutral prior; empirical role win rates come from the crawler later.
        return 50.0;
    }

    @Override
    public double synergy(int championA, int championB) {
        Champion a = champions.byId(championA).orElse(null);
        Champion b = champions.byId(championB).orElse(null);
        if (a == null || b == null || a.id() == b.id()) {
            return 0.0;
        }
        double s = 0.0;
        if (a.damageType() != DamageType.MIXED && b.damageType() != DamageType.MIXED
                && a.damageType() != b.damageType()) {
            s += 1.5; // mixed AD/AP threat is harder to itemize against
        }
        if ((a.isFrontline() && b.isCarry()) || (b.isFrontline() && a.isCarry())) {
            s += 1.5; // a front line to protect a carry
        }
        if ((a.cc() && b.isCarry()) || (b.cc() && a.isCarry())) {
            s += 1.0; // crowd control sets up the carry
        }
        if (a.isCarry() && b.isCarry() && !a.isFrontline() && !b.isFrontline()) {
            s -= 1.0; // two squishies with no front line
        }
        return round1(clamp(s, -3.0, 3.0));
    }

    @Override
    public double counter(int championId, int vsChampionId) {
        Champion c = champions.byId(championId).orElse(null);
        Champion v = champions.byId(vsChampionId).orElse(null);
        if (c == null || v == null || c.id() == v.id()) {
            return 0.0;
        }
        return round1(clamp(advantage(c, v) - advantage(v, c), -4.0, 4.0));
    }

    /** How much {@code attacker} is favoured into {@code defender}, in percentage points (>= 0). */
    private static double advantage(Champion attacker, Champion defender) {
        double a = 0.0;
        boolean defSquishyCarry = (defender.hasTag(ChampionTag.MAGE) || defender.hasTag(ChampionTag.MARKSMAN))
                && !defender.hasTag(ChampionTag.TANK);
        if (attacker.hasTag(ChampionTag.ASSASSIN) && defSquishyCarry) {
            a += 2.5; // assassins delete squishies
        }
        if ((attacker.hasTag(ChampionTag.TANK) || attacker.hasTag(ChampionTag.FIGHTER))
                && defender.hasTag(ChampionTag.ASSASSIN)) {
            a += 2.0; // bruisers/tanks survive assassins
        }
        if ((attacker.hasTag(ChampionTag.MARKSMAN) || attacker.hasTag(ChampionTag.MAGE))
                && defender.hasTag(ChampionTag.TANK) && !defender.hasTag(ChampionTag.MAGE)) {
            a += 1.5; // ranged poke wears down immobile tanks
        }
        if (attacker.cc() && defender.hasTag(ChampionTag.ASSASSIN)) {
            a += 1.0; // reliable CC locks down divers
        }
        return a;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
