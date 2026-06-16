package gg.leaguetool.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Aggregated games/wins for a same-team champion pair. The pair is stored with
 * {@code championLow < championHigh} so each unordered pair has a single row.
 */
@Entity
@Table(name = "champion_pair_stat", uniqueConstraints =
        @UniqueConstraint(name = "uk_pair_stat", columnNames = {"champion_low", "champion_high"}))
public class ChampionPairStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int championLow;
    private int championHigh;
    private long games;
    private long wins;

    protected ChampionPairStat() {
    }

    public ChampionPairStat(int championLow, int championHigh) {
        this.championLow = championLow;
        this.championHigh = championHigh;
    }

    public static int low(int a, int b) {
        return Math.min(a, b);
    }

    public static int high(int a, int b) {
        return Math.max(a, b);
    }

    public void addGame(boolean win) {
        games++;
        if (win) {
            wins++;
        }
    }

    public long getGames() {
        return games;
    }

    public double winRate() {
        return games == 0 ? 50.0 : 100.0 * wins / games;
    }
}
