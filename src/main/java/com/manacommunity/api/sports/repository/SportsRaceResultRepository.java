package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.RaceStatus;
import com.manacommunity.api.sports.scheduler.SportsRaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SportsRaceResultRepository extends JpaRepository<SportsRaceResult, Long> {

    List<SportsRaceResult> findByMatchIdOrderByOverallRankAsc(Long matchId);

    List<SportsRaceResult> findByMatchIdAndHeatNumberOrderByHeatRankAsc(Long matchId, Integer heatNumber);

    List<SportsRaceResult> findByPlayerIdOrderByCreatedAtDesc(Long playerId);

    List<SportsRaceResult> findByMatchIdAndRaceStatusOrderByFinishTimeMillisAsc(Long matchId, RaceStatus raceStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsRaceResult r WHERE r.match.id IN " +
           "(SELECT m.id FROM SportsTournamentMatch m WHERE m.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
