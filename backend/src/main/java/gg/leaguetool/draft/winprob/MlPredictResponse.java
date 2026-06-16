package gg.leaguetool.draft.winprob;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body from the ML service {@code POST /predict} (snake_case mapped to camelCase). */
record MlPredictResponse(
        @JsonProperty("win_probability") double winProbability,
        @JsonProperty("model_loaded") boolean modelLoaded,
        String note) {
}
