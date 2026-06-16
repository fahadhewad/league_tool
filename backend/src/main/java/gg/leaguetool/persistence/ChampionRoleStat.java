package gg.leaguetool.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Aggregated games/wins for a champion in a role → empirical role win rate. */
@Entity
@Table(name = "champion_role_stat", uniqueConstraints =
        @UniqueConstraint(name = "uk_role_stat", columnNames = {"champion_id", "role_name"}))
public class ChampionRoleStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int championId;

    @Column(name = "role_name")
    private String role;
    private long games;
    private long wins;

    protected ChampionRoleStat() {
    }

    public ChampionRoleStat(int championId, String role) {
        this.championId = championId;
        this.role = role;
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
