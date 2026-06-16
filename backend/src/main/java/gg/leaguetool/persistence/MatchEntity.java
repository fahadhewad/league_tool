package gg.leaguetool.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A crawled match (header). Participants are stored separately. */
@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @Column(length = 32)
    private String matchId;

    private int queueId;
    private String gameVersion;
    private long gameDuration;
    private long gameCreation;
    private Integer winningTeam;
    private Instant ingestedAt;

    protected MatchEntity() {
    }

    public MatchEntity(String matchId, int queueId, String gameVersion, long gameDuration,
                       long gameCreation, Integer winningTeam, Instant ingestedAt) {
        this.matchId = matchId;
        this.queueId = queueId;
        this.gameVersion = gameVersion;
        this.gameDuration = gameDuration;
        this.gameCreation = gameCreation;
        this.winningTeam = winningTeam;
        this.ingestedAt = ingestedAt;
    }

    public String getMatchId() {
        return matchId;
    }

    public int getQueueId() {
        return queueId;
    }

    public Integer getWinningTeam() {
        return winningTeam;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
