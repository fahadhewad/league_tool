package gg.leaguetool.form;

import gg.leaguetool.common.Platform;
import gg.leaguetool.form.model.RecentForm;
import gg.leaguetool.profile.model.MatchSummary;
import gg.leaguetool.riot.dto.AccountDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentFormServiceTest {

    private final RecentFormService service = new RecentFormService(null, null);
    private final AccountDto account = new AccountDto("PUUID-X", "Agurin", "EUW");

    private static MatchSummary game(boolean win, int k, int d, int a) {
        double kda = d == 0 ? (k + a) : Math.round((double) (k + a) / d * 100.0) / 100.0;
        return new MatchSummary("EUW1_" + System.nanoTime(), 420, "CLASSIC", "LeeSin", "JUNGLE",
                win, k, d, a, kda, 1800, 0L, 20000, 12000, 200);
    }

    @Test
    void flagsTiltOnRecentLossStreak() {
        // Most-recent first: three losses, then three wins.
        List<MatchSummary> matches = List.of(
                game(false, 2, 8, 3), game(false, 1, 7, 4), game(false, 3, 9, 2),
                game(true, 8, 2, 10), game(true, 9, 1, 8), game(true, 7, 3, 11));

        RecentForm form = service.compute(account, Platform.EUW1, matches);

        assertThat(form.gamesAnalyzed()).isEqualTo(6);
        assertThat(form.wins()).isEqualTo(3);
        assertThat(form.losses()).isEqualTo(3);
        assertThat(form.winRate()).isEqualTo(50.0);
        assertThat(form.currentStreak()).isEqualTo(-3);
        assertThat(form.tiltAlert()).isTrue();
        assertThat(form.breakdown()).hasSize(4);
        assertThat(form.games()).hasSize(6);
    }

    @Test
    void rewardsAWinStreakWithAHighScore() {
        List<MatchSummary> matches = List.of(
                game(true, 8, 2, 10), game(true, 9, 1, 8), game(true, 7, 3, 11),
                game(true, 10, 2, 9), game(true, 6, 1, 12));

        RecentForm form = service.compute(account, Platform.EUW1, matches);

        assertThat(form.currentStreak()).isEqualTo(5);
        assertThat(form.winRate()).isEqualTo(100.0);
        assertThat(form.tiltAlert()).isFalse();
        assertThat(form.formScore()).isGreaterThanOrEqualTo(85.0);
        assertThat(form.formLabel()).isEqualTo("On fire");
    }

    @Test
    void handlesNoRecentGames() {
        RecentForm form = service.compute(account, Platform.EUW1, List.of());

        assertThat(form.gamesAnalyzed()).isZero();
        assertThat(form.formLabel()).isEqualTo("No recent games");
        assertThat(form.formScore()).isEqualTo(50.0);
        assertThat(form.tiltAlert()).isFalse();
    }
}
