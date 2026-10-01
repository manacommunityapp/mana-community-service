package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsPlayerMatchStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsPlayerMatchStatsRepository extends JpaRepository<SportsPlayerMatchStats, Long> {

    List<SportsPlayerMatchStats> findByMatchId(Long matchId);

    Optional<SportsPlayerMatchStats> findByMatchIdAndPlayerId(Long matchId, Long playerId);

    List<SportsPlayerMatchStats> findByPlayerIdAndSportType(Long playerId, String sportType);
}
