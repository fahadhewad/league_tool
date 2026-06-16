package gg.leaguetool.draft.winprob;

import gg.leaguetool.draft.model.DraftPick;
import gg.leaguetool.draft.model.WinProbability;
import org.springframework.stereotype.Service;

import java.util.List;

/** Maps draft picks to champion ids and asks the ML service for a win probability. */
@Service
public class WinProbabilityService {

    private final WinProbabilityClient client;

    public WinProbabilityService(WinProbabilityClient client) {
        this.client = client;
    }

    public WinProbability estimate(List<DraftPick> allies, List<DraftPick> enemies) {
        return client.predict(championIds(allies), championIds(enemies));
    }

    private static List<Integer> championIds(List<DraftPick> picks) {
        return picks == null ? List.of() : picks.stream().map(DraftPick::championId).toList();
    }
}
