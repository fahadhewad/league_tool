package gg.leaguetool.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChampionMatchupStatRepository extends JpaRepository<ChampionMatchupStat, Long> {

    Optional<ChampionMatchupStat> findByChampionIdAndOpponentChampionIdAndRole(
            int championId, int opponentChampionId, String role);

    /** Total games of a champion into an opponent across all roles. */
    @Query("select coalesce(sum(m.games), 0) from ChampionMatchupStat m "
            + "where m.championId = :champ and m.opponentChampionId = :opponent")
    long totalGames(@Param("champ") int champ, @Param("opponent") int opponent);

    /** Total wins of a champion into an opponent across all roles. */
    @Query("select coalesce(sum(m.wins), 0) from ChampionMatchupStat m "
            + "where m.championId = :champ and m.opponentChampionId = :opponent")
    long totalWins(@Param("champ") int champ, @Param("opponent") int opponent);
}
