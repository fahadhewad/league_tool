package gg.leaguetool.draft;

import gg.leaguetool.draft.model.DraftAnalysis;
import gg.leaguetool.draft.model.DraftAnalyzeRequest;
import gg.leaguetool.draft.model.DraftRecommendRequest;
import gg.leaguetool.draft.model.PickRecommendations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Draft analysis and the champion pick recommender for your own champ select. */
@RestController
@RequestMapping("/api/v1/draft")
@Tag(name = "Draft", description = "Synergy/counter analysis and champion pick recommendations")
public class DraftController {

    private final DraftAnalyzerService analyzer;
    private final PickRecommenderService recommender;

    public DraftController(DraftAnalyzerService analyzer, PickRecommenderService recommender) {
        this.analyzer = analyzer;
        this.recommender = recommender;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze a champ-select state: comp, synergy, counters, coverage")
    public DraftAnalysis analyze(@Valid @RequestBody DraftAnalyzeRequest request) {
        return analyzer.analyze(request.alliesOrEmpty(), request.enemiesOrEmpty());
    }

    @PostMapping("/recommend")
    @Operation(summary = "Recommend which champion to pick for your open role")
    public PickRecommendations recommend(@Valid @RequestBody DraftRecommendRequest request) {
        return recommender.recommend(request.role(), request.alliesOrEmpty(),
                request.enemiesOrEmpty(), request.bansOrEmpty());
    }
}
