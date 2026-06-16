package gg.leaguetool.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** One player's line in a crawled match. */
@Entity
@Table(name = "match_participant", indexes = {
        @Index(name = "idx_participant_match", columnList = "match_id"),
        @Index(name = "idx_participant_champion", columnList = "champion_id")
})
public class MatchParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String matchId;
    private String puuid;
    private int championId;
    private int teamId;
    private String teamPosition;
    private boolean win;
    private int kills;
    private int deaths;
    private int assists;

    protected MatchParticipantEntity() {
    }

    public MatchParticipantEntity(String matchId, String puuid, int championId, int teamId,
                                  String teamPosition, boolean win, int kills, int deaths, int assists) {
        this.matchId = matchId;
        this.puuid = puuid;
        this.championId = championId;
        this.teamId = teamId;
        this.teamPosition = teamPosition;
        this.win = win;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
    }

    public int getChampionId() {
        return championId;
    }

    public int getTeamId() {
        return teamId;
    }

    public String getTeamPosition() {
        return teamPosition;
    }

    public boolean isWin() {
        return win;
    }

    public String getPuuid() {
        return puuid;
    }
}
