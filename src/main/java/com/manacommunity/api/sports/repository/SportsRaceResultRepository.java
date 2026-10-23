package com.manacommunity.api.sports.repository;
import com.manacommunity.api.sports.model.RaceStatus;


import com.manacommunity.api.sports.scheduler.SportsRaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsRaceResultRepository extends JpaRepository<SportsRaceResult, Long> {

    List<SportsRaceResult> findByMatchIdOrderByOverallRankAsc(Long matchId);

    List<SportsRaceResult> findByMatchIdAndHeatNumberOrderByHeatRankAsc(Long matchId, Integer heatNumber);

    List<SportsRaceResult> findByPlayerIdOrderByCreatedAtDesc(Long playerId);

    List<SportsRaceResult> findByMatchIdAndRaceStatusOrderByFinishTimeMillisAsc(Long matchId, com.manacommunity.api.sports.model.RaceStatus raceStatus);
}
