package gg.leaguetool.draft;

import gg.leaguetool.champion.Champion;
import gg.leaguetool.champion.ChampionRepository;
import gg.leaguetool.draft.model.CompSummary;
import gg.leaguetool.draft.model.CounterNote;
import gg.leaguetool.draft.model.DraftAnalysis;
import gg.leaguetool.draft.model.DraftPick;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Analyzes a champ-select state from your team's perspective: composition snapshots, total pairwise
 * synergy, the matchups where an enemy is favoured into one of your champions, and suggestions to
 * round out the comp.
 */
@Service
public class DraftAnalyzerService {

    private static final double THREAT_THRESHOLD = 1.5;

    private final ChampionRepository champions;
    private final DraftDataProvider data;

    public DraftAnalyzerService(ChampionRepository champions, DraftDataProvider data) {
        this.champions = champions;
        this.data = data;
    }

    public DraftAnalysis analyze(List<DraftPick> allyPicks, List<DraftPick> enemyPicks) {
        List<Champion> allies = resolve(allyPicks);
        List<Champion> enemies = resolve(enemyPicks);

        CompSummary allyComp = CompAnalyzer.summarize(allies);
        CompSummary enemyComp = CompAnalyzer.summarize(enemies);

        double synergyTotal = 0.0;
        List<SynergyPair> pairs = new ArrayList<>();
        for (int i = 0; i < allies.size(); i++) {
            for (int j = i + 1; j < allies.size(); j++) {
                double s = data.synergy(allies.get(i).id(), allies.get(j).id());
                synergyTotal += s;
                if (s != 0) {
                    pairs.add(new SynergyPair(allies.get(i), allies.get(j), s));
                }
            }
        }
        synergyTotal = Math.round(synergyTotal * 10.0) / 10.0;

        List<String> synergyNotes = pairs.stream()
                .filter(p -> p.score() > 0)
                .sorted(Comparator.comparingDouble(SynergyPair::score).reversed())
                .limit(3)
                .map(p -> p.a().name() + " + " + p.b().name() + ": +" + p.score() + " synergy")
                .toList();

        List<CounterNote> threats = findThreats(allies, enemies);
        List<String> recommendations = recommend(allyComp, threats);

        return new DraftAnalysis(allyComp, enemyComp, synergyTotal, synergyNotes, threats, recommendations);
    }

    private List<CounterNote> findThreats(List<Champion> allies, List<Champion> enemies) {
        List<CounterNote> threats = new ArrayList<>();
        for (Champion ally : allies) {
            for (Champion enemy : enemies) {
                double advantage = data.counter(ally.id(), enemy.id());
                if (advantage <= -THREAT_THRESHOLD) {
                    threats.add(new CounterNote(ally.id(), ally.name(), enemy.id(), enemy.name(),
                            Math.abs(advantage), enemy.name() + " is favoured into " + ally.name()));
                }
            }
        }
        threats.sort(Comparator.comparingDouble(CounterNote::disadvantage).reversed());
        return threats.stream().limit(5).toList();
    }

    private static List<String> recommend(CompSummary allyComp, List<CounterNote> threats) {
        List<String> out = new ArrayList<>();
        if (allyComp.championCount() == 0) {
            return out;
        }
        if (allyComp.frontlineCount() == 0) {
            out.add("Add a front line — your comp has no tank or bruiser to absorb damage.");
        }
        if (allyComp.ccCount() <= 1) {
            out.add("Pick up more crowd control to lock targets down.");
        }
        if ("AD-heavy".equals(allyComp.damageBalance())) {
            out.add("Your damage is AD-heavy; an AP threat makes you harder to itemize against.");
        } else if ("AP-heavy".equals(allyComp.damageBalance())) {
            out.add("Your damage is AP-heavy; add an AD threat for mixed pressure.");
        }
        if (!threats.isEmpty()) {
            CounterNote worst = threats.get(0);
            out.add("Watch the " + worst.allyName() + " matchup — " + worst.enemyName()
                    + " is favoured into them.");
        }
        return out;
    }

    private List<Champion> resolve(List<DraftPick> picks) {
        if (picks == null) {
            return List.of();
        }
        return picks.stream()
                .map(p -> champions.byId(p.championId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private record SynergyPair(Champion a, Champion b, double score) {
    }
}
