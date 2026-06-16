package gg.leaguetool.form.model;

/**
 * One transparent contributor to the overall form score.
 *
 * @param factor  short name (e.g. "Win rate")
 * @param score   this factor's score on a 0–100 scale
 * @param weight  its weight in the composite (weights sum to 1.0)
 * @param detail  human-readable explanation of the value
 */
public record FormFactor(String factor, double score, double weight, String detail) {
}
