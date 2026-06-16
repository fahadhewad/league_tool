package gg.leaguetool.draft.winprob;

import gg.leaguetool.draft.model.WinProbability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Calls the Python ML service for a comp-vs-comp win probability. Any failure (service down, slow,
 * or error) degrades to a neutral {@link WinProbability#unavailable()} so champ select is never
 * blocked by the ML layer.
 */
@Component
public class WinProbabilityClient {

    private static final Logger log = LoggerFactory.getLogger(WinProbabilityClient.class);

    private final RestClient ml;

    public WinProbabilityClient(RestClient mlRestClient) {
        this.ml = mlRestClient;
    }

    public WinProbability predict(List<Integer> allies, List<Integer> enemies) {
        try {
            MlPredictResponse response = ml.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MlPredictRequest(allies, enemies))
                    .retrieve()
                    .body(MlPredictResponse.class);
            if (response == null) {
                return WinProbability.unavailable();
            }
            double rounded = Math.round(response.winProbability() * 10000.0) / 10000.0;
            String source = response.modelLoaded() ? "ml-model" : "ml-fallback";
            return new WinProbability(rounded, response.modelLoaded(), source);
        } catch (Exception e) {
            log.warn("ML service /predict unavailable: {}", e.toString());
            return WinProbability.unavailable();
        }
    }

    private record MlPredictRequest(List<Integer> allies, List<Integer> enemies) {
    }
}
