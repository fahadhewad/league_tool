package gg.leaguetool.draft;

import gg.leaguetool.champion.Champion;
import gg.leaguetool.champion.DamageType;
import gg.leaguetool.draft.model.CompSummary;

import java.util.ArrayList;
import java.util.List;

/** Builds a {@link CompSummary} from a resolved list of champions. */
final class CompAnalyzer {

    private CompAnalyzer() {
    }

    static CompSummary summarize(List<Champion> champions) {
        int physical = 0;
        int magic = 0;
        int mixed = 0;
        int frontline = 0;
        int cc = 0;
        int carry = 0;
        for (Champion c : champions) {
            switch (c.damageType()) {
                case PHYSICAL -> physical++;
                case MAGIC -> magic++;
                case MIXED -> mixed++;
            }
            if (c.isFrontline()) {
                frontline++;
            }
            if (c.cc()) {
                cc++;
            }
            if (c.isCarry()) {
                carry++;
            }
        }

        String balance = damageBalance(physical, magic, mixed);

        List<String> notes = new ArrayList<>();
        if (!champions.isEmpty()) {
            if (frontline == 0) {
                notes.add("No reliable front line");
            }
            if (cc == 0) {
                notes.add("No reliable crowd control");
            } else if (cc == 1) {
                notes.add("Light on crowd control");
            }
            if (!"Balanced".equals(balance)) {
                notes.add(balance + " damage profile");
            }
        }

        return new CompSummary(champions.size(), physical, magic, mixed, frontline, cc, carry, balance, notes);
    }

    static String damageBalance(int physical, int magic, int mixed) {
        double physicalWeight = physical + mixed * 0.5;
        double magicWeight = magic + mixed * 0.5;
        if (Math.abs(physicalWeight - magicWeight) <= 0.5) {
            return "Balanced";
        }
        return physicalWeight > magicWeight ? "AD-heavy" : "AP-heavy";
    }

    /** The damage type a comp most needs more of, or {@code null} if reasonably balanced. */
    static DamageType desiredDamageType(int physical, int magic, int mixed) {
        double physicalWeight = physical + mixed * 0.5;
        double magicWeight = magic + mixed * 0.5;
        if (physicalWeight > magicWeight + 0.5) {
            return DamageType.MAGIC;
        }
        if (magicWeight > physicalWeight + 0.5) {
            return DamageType.PHYSICAL;
        }
        return null;
    }
}
