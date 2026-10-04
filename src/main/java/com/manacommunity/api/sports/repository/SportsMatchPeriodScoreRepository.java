package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchPeriodScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsMatchPeriodScoreRepository extends JpaRepository<SportsMatchPeriodScore, Long> {

    List<SportsMatchPeriodScore> findByMatchIdOrderByPeriodNumber(Long matchId);

    void deleteByMatchId(Long matchId);
}
