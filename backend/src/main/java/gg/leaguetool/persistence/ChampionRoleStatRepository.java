package gg.leaguetool.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChampionRoleStatRepository extends JpaRepository<ChampionRoleStat, Long> {

    Optional<ChampionRoleStat> findByChampionIdAndRole(int championId, String role);
}
