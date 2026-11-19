package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsPlayerMatchStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsPlayerMatchStatsRepository extends JpaRepository<SportsPlayerMatchStats, Long> {

    List<SportsPlayerMatchStats> findByMatchId(Long matchId);

    Optional<SportsPlayerMatchStats> findByMatchIdAndPlayerId(Long matchId, Long playerId);

    List<SportsPlayerMatchStats> findByPlayerIdAndSportType(Long playerId, String sportType);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsPlayerMatchStats s WHERE s.match.id IN " +
           "(SELECT m.id FROM SportsTournamentMatch m WHERE m.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
