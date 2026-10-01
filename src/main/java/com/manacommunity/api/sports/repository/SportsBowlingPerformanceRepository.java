package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsBowlingPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SportsBowlingPerformanceRepository extends JpaRepository<SportsBowlingPerformance, Long> {
    List<SportsBowlingPerformance> findByInningsIdOrderByBowlingOrder(Long inningsId);
    List<SportsBowlingPerformance> findByPlayerIdAndInnings_MatchResult_Match_Config_Id(Long playerId, Long configId);
}
