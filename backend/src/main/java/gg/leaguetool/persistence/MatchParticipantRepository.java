package gg.leaguetool.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipantEntity, Long> {

    List<MatchParticipantEntity> findByMatchId(String matchId);
}
