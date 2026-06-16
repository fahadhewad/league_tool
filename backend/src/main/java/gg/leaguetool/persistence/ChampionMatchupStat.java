package gg.leaguetool.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Aggregated games/wins for a champion against a lane opponent in a role (directed). The opposite
 * direction is stored as its own row, so {@code winRate} is the champion's win rate into the opponent.
 */
@Entity
@Table(name = "champion_matchup_stat", uniqueConstraints = @UniqueConstraint(
        name = "uk_matchup_stat", columnNames = {"champion_id", "opponent_champion_id", "role_name"}))
public class ChampionMatchupStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int championId;
    private int opponentChampionId;

    @Column(name = "role_name")
    private String role;
    private long games;
    private long wins;

    protected ChampionMatchupStat() {
    }

    public ChampionMatchupStat(int championId, int opponentChampionId, String role) {
        this.championId = championId;
        this.opponentChampionId = opponentChampionId;
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
