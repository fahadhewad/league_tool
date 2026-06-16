package gg.leaguetool.form.model;

/** Compact per-game view used in the recent-form breakdown. */
public record GameForm(
        String matchId,
        String championName,
        int queueId,
        boolean win,
        int kills,
        int deaths,
        int assists,
        double kda) {
}
