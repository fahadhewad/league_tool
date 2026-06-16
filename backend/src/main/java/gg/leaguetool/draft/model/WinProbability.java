package gg.leaguetool.draft.model;

/**
 * Comp-vs-comp win-probability estimate from the ML service.
 *
 * @param winProbability probability (0–1) that the ally team wins
 * @param modelLoaded    whether a trained model produced this (false = neutral fallback)
 * @param source         where the number came from: "ml-model", "ml-fallback", or "unavailable"
 */
public record WinProbability(double winProbability, boolean modelLoaded, String source) {

    public static WinProbability unavailable() {
        return new WinProbability(0.5, false, "unavailable");
    }
}
