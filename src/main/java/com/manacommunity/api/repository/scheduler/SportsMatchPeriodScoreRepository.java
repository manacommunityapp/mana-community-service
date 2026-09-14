package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsMatchPeriodScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsMatchPeriodScoreRepository extends JpaRepository<SportsMatchPeriodScore, Long> {

    List<SportsMatchPeriodScore> findByMatchIdOrderByPeriodNumber(Long matchId);

    void deleteByMatchId(Long matchId);
}
