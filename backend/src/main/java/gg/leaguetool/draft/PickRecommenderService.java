package gg.leaguetool.draft;

import gg.leaguetool.champion.Champion;
import gg.leaguetool.champion.ChampionRepository;
import gg.leaguetool.champion.DamageType;
import gg.leaguetool.champion.Role;
import gg.leaguetool.draft.model.CompSummary;
import gg.leaguetool.draft.model.DraftPick;
import gg.leaguetool.draft.model.PickRecommendation;
import gg.leaguetool.draft.model.PickRecommendations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Recommends which champion to pick for your open role given the allies and enemies already locked
 * in. Each candidate is scored as a transparent blend of synergy with your team, counter advantage
 * against the enemies (weighting your direct lane opponent more heavily), and how well it fills your
 * comp's damage / frontline / CC needs — every recommendation comes with its reasons.
 */
@Service
public class PickRecommenderService {

    private static final double W_SYNERGY = 2.0;
    private static final double W_COUNTER = 2.0;
    private static final double W_FIT = 1.5;
    private static final int MAX_RESULTS = 10;

    /**
     * Total ordering for recommendations: best overall score first, then the strongest individual
     * components, with champion id as a final stable tiebreaker. Making the order fully deterministic
     * means equally-scored picks rank the same way regardless of the roster's iteration order.
     */
    private static final Comparator<PickRecommendation> RANKING =
            Comparator.comparingDouble(PickRecommendation::score).reversed()
                    .thenComparing(Comparator.comparingDouble(PickRecommendation::counterScore).reversed())
                    .thenComparing(Comparator.comparingDouble(PickRecommendation::synergyScore).reversed())
                    .thenComparing(Comparator.comparingDouble(PickRecommendation::damageFitScore).reversed())
                    .thenComparingInt(PickRecommendation::championId);

    private final ChampionRepository champions;
    private final DraftDataProvider data;

    public PickRecommenderService(ChampionRepository champions, DraftDataProvider data) {
        this.champions = champions;
        this.data = data;
    }

    public PickRecommendations recommend(Role role, List<DraftPick> allyPicks,
                                         List<DraftPick> enemyPicks, List<Integer> bans) {
        List<Champion> allies = resolve(allyPicks);
        List<Champion> enemies = resolve(enemyPicks);
        Champion laneOpponent = laneOpponent(enemyPicks, role);

        Set<Integer> excluded = new HashSet<>(bans == null ? List.of() : bans);
        allies.forEach(c -> excluded.add(c.id()));
        enemies.forEach(c -> excluded.add(c.id()));

        CompSummary allyComp = CompAnalyzer.summarize(allies);

        List<PickRecommendation> recs = champions.byRole(role).stream()
                .filter(c -> !excluded.contains(c.id()))
                .map(c -> score(c, role, allies, enemies, laneOpponent, allyComp))
                .sorted(RANKING)
                .limit(MAX_RESULTS)
                .toList();

        return new PickRecommendations(role, recs);
    }

    private PickRecommendation score(Champion candidate, Role role, List<Champion> allies,
                                     List<Champion> enemies, Champion laneOpponent, CompSummary allyComp) {
        double synergy = averageSynergy(candidate, allies);
        double counter = weightedCounter(candidate, enemies, laneOpponent);

        DamageType desired = CompAnalyzer.desiredDamageType(
                allyComp.physicalCount(), allyComp.magicCount(), allyComp.mixedCount());
        double fit = damageFit(candidate, allyComp, desired);

        double baseAdjustment = data.baseWinRate(candidate.id(), role) - 50.0;
        double score = round1(50.0 + W_SYNERGY * synergy + W_COUNTER * counter + W_FIT * fit + baseAdjustment);

        List<String> reasons = reasons(candidate, allyComp, desired, synergy, counter, laneOpponent, role);

        return new PickRecommendation(candidate.id(), candidate.name(), role, score,
                round1(synergy), round1(counter), round1(fit), reasons);
    }

    private double averageSynergy(Champion candidate, List<Champion> allies) {
        if (allies.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Champion ally : allies) {
            sum += data.synergy(candidate.id(), ally.id());
        }
        return sum / allies.size();
    }

    private double weightedCounter(Champion candidate, List<Champion> enemies, Champion laneOpponent) {
        if (enemies.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        double weight = 0.0;
        for (Champion enemy : enemies) {
            double w = (laneOpponent != null && enemy.id() == laneOpponent.id()) ? 2.0 : 1.0;
            sum += data.counter(candidate.id(), enemy.id()) * w;
            weight += w;
        }
        return weight == 0 ? 0.0 : sum / weight;
    }

    private static double damageFit(Champion candidate, CompSummary allyComp, DamageType desired) {
        if (allyComp.championCount() == 0) {
            return 0.0;
        }
        double fit = 0.0;
        if (candidate.damageType() == DamageType.MIXED) {
            fit += 0.5;
        } else if (desired != null) {
            if (candidate.damageType() == desired) {
                fit += 1.5;
            } else {
                fit -= 1.0; // doubles down on the over-represented damage type
            }
        }
        if (allyComp.frontlineCount() == 0 && candidate.isFrontline()) {
            fit += 1.5;
        }
        if (allyComp.ccCount() <= 1 && candidate.cc()) {
            fit += 1.0;
        }
        return Math.max(-2.0, Math.min(4.0, fit));
    }

    private static List<String> reasons(Champion candidate, CompSummary allyComp, DamageType desired,
                                        double synergy, double counter, Champion laneOpponent, Role role) {
        List<String> reasons = new ArrayList<>();
        if (synergy >= 1.0) {
            reasons.add("Strong synergy with your team (+" + round1(synergy) + ")");
        } else if (synergy <= -1.0) {
            reasons.add("Weak synergy with your current picks (" + round1(synergy) + ")");
        }
        if (counter >= 1.0) {
            reasons.add(laneOpponent != null
                    ? "Favoured into enemy " + laneOpponent.name()
                    : "Favoured into the enemy comp");
        } else if (counter <= -1.0) {
            reasons.add("Risky into the enemy comp (" + round1(counter) + ")");
        }
        if (allyComp.championCount() > 0 && desired != null
                && candidate.damageType() == desired) {
            reasons.add("Fills your " + (desired == DamageType.MAGIC ? "magic" : "physical") + " damage gap");
        }
        if (allyComp.frontlineCount() == 0 && candidate.isFrontline()) {
            reasons.add("Adds a front line your comp is missing");
        }
        if (allyComp.ccCount() <= 1 && candidate.cc()) {
            reasons.add("Brings crowd control your comp lacks");
        }
        if (reasons.isEmpty()) {
            reasons.add("Solid, flexible pick for " + role);
        }
        return reasons;
    }

    private Champion laneOpponent(List<DraftPick> enemyPicks, Role role) {
        if (enemyPicks == null) {
            return null;
        }
        return enemyPicks.stream()
                .filter(p -> p.role() == role)
                .map(p -> champions.byId(p.championId()).orElse(null))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<Champion> resolve(List<DraftPick> picks) {
        if (picks == null) {
            return List.of();
        }
        return picks.stream()
                .map(p -> champions.byId(p.championId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
