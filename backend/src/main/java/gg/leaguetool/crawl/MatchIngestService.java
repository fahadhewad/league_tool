package gg.leaguetool.crawl;

import gg.leaguetool.champion.Role;
import gg.leaguetool.persistence.ChampionMatchupStat;
import gg.leaguetool.persistence.ChampionMatchupStatRepository;
import gg.leaguetool.persistence.ChampionPairStat;
import gg.leaguetool.persistence.ChampionPairStatRepository;
import gg.leaguetool.persistence.ChampionRoleStat;
import gg.leaguetool.persistence.ChampionRoleStatRepository;
import gg.leaguetool.persistence.MatchEntity;
import gg.leaguetool.persistence.MatchParticipantEntity;
import gg.leaguetool.persistence.MatchParticipantRepository;
import gg.leaguetool.persistence.MatchRepository;
import gg.leaguetool.riot.dto.MatchDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persists a crawled match and incrementally updates the champion role / pair / matchup aggregates
 * that power the empirical {@link gg.leaguetool.draft.DbDraftDataProvider}. Idempotent: a match that
 * is already stored is skipped.
 */
@Service
public class MatchIngestService {

    private final MatchRepository matches;
    private final MatchParticipantRepository participants;
    private final ChampionRoleStatRepository roleStats;
    private final ChampionPairStatRepository pairStats;
    private final ChampionMatchupStatRepository matchupStats;

    public MatchIngestService(MatchRepository matches, MatchParticipantRepository participants,
                              ChampionRoleStatRepository roleStats, ChampionPairStatRepository pairStats,
                              ChampionMatchupStatRepository matchupStats) {
        this.matches = matches;
        this.participants = participants;
        this.roleStats = roleStats;
        this.pairStats = pairStats;
        this.matchupStats = matchupStats;
    }

    /** @return true if the match was newly ingested, false if skipped (already present / invalid). */
    @Transactional
    public boolean ingest(MatchDto match) {
        if (match == null || match.metadata() == null || match.info() == null) {
            return false;
        }
        String matchId = match.metadata().matchId();
        if (matchId == null || matches.existsById(matchId)) {
            return false;
        }
        MatchDto.Info info = match.info();
        List<MatchDto.Participant> ps = info.participants();
        if (ps == null || ps.isEmpty()) {
            return false;
        }

        Integer winningTeam = ps.stream().filter(MatchDto.Participant::win)
                .map(MatchDto.Participant::teamId).findFirst().orElse(null);
        matches.save(new MatchEntity(matchId, info.queueId(), info.gameVersion(),
                info.gameDuration(), info.gameCreation(), winningTeam, Instant.now()));

        for (MatchDto.Participant p : ps) {
            participants.save(new MatchParticipantEntity(matchId, p.puuid(), p.championId(),
                    p.teamId(), p.teamPosition(), p.win(), p.kills(), p.deaths(), p.assists()));
        }

        updateRoleStats(ps);
        updatePairStats(ps);
        updateMatchupStats(ps);
        return true;
    }

    private void updateRoleStats(List<MatchDto.Participant> ps) {
        for (MatchDto.Participant p : ps) {
            if (Role.fromTeamPosition(p.teamPosition()) == null) {
                continue;
            }
            ChampionRoleStat stat = roleStats.findByChampionIdAndRole(p.championId(), p.teamPosition())
                    .orElseGet(() -> new ChampionRoleStat(p.championId(), p.teamPosition()));
            stat.addGame(p.win());
            roleStats.save(stat);
        }
    }

    private void updatePairStats(List<MatchDto.Participant> ps) {
        Map<Integer, List<MatchDto.Participant>> byTeam =
                ps.stream().collect(Collectors.groupingBy(MatchDto.Participant::teamId));
        for (List<MatchDto.Participant> team : byTeam.values()) {
            for (int i = 0; i < team.size(); i++) {
                for (int j = i + 1; j < team.size(); j++) {
                    int a = team.get(i).championId();
                    int b = team.get(j).championId();
                    int low = ChampionPairStat.low(a, b);
                    int high = ChampionPairStat.high(a, b);
                    if (low == high) {
                        continue;
                    }
                    ChampionPairStat stat = pairStats.findByChampionLowAndChampionHigh(low, high)
                            .orElseGet(() -> new ChampionPairStat(low, high));
                    stat.addGame(team.get(i).win());
                    pairStats.save(stat);
                }
            }
        }
    }

    private void updateMatchupStats(List<MatchDto.Participant> ps) {
        Map<String, List<MatchDto.Participant>> byPosition = ps.stream()
                .filter(p -> Role.fromTeamPosition(p.teamPosition()) != null)
                .collect(Collectors.groupingBy(MatchDto.Participant::teamPosition));
        for (Map.Entry<String, List<MatchDto.Participant>> entry : byPosition.entrySet()) {
            List<MatchDto.Participant> laners = entry.getValue();
            if (laners.size() != 2 || laners.get(0).teamId() == laners.get(1).teamId()) {
                continue;
            }
            recordMatchup(laners.get(0), laners.get(1), entry.getKey());
            recordMatchup(laners.get(1), laners.get(0), entry.getKey());
        }
    }

    private void recordMatchup(MatchDto.Participant champ, MatchDto.Participant opponent, String role) {
        ChampionMatchupStat stat = matchupStats
                .findByChampionIdAndOpponentChampionIdAndRole(champ.championId(), opponent.championId(), role)
                .orElseGet(() -> new ChampionMatchupStat(champ.championId(), opponent.championId(), role));
        stat.addGame(champ.win());
        matchupStats.save(stat);
    }
}
