package gg.leaguetool.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChampionPairStatRepository extends JpaRepository<ChampionPairStat, Long> {

    Optional<ChampionPairStat> findByChampionLowAndChampionHigh(int championLow, int championHigh);
}
