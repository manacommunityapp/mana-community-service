package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsPlayerTournamentStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsPlayerTournamentStatsRepository extends JpaRepository<SportsPlayerTournamentStats, Long> {
    Optional<SportsPlayerTournamentStats> findByConfigIdAndPlayerId(Long configId, Long playerId);
    List<SportsPlayerTournamentStats> findByConfigIdOrderByTotalRunsDesc(Long configId);
    List<SportsPlayerTournamentStats> findByConfigIdOrderByTotalWicketsDesc(Long configId);
    List<SportsPlayerTournamentStats> findByConfigIdOrderByManOfMatchCountDesc(Long configId);
    List<SportsPlayerTournamentStats> findByPlayerId(Long playerId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsPlayerTournamentStats s WHERE s.config.id = :configId")
    void deleteByConfigId(@Param("configId") Long configId);
}
