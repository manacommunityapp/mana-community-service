package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.BattingPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BattingPerformanceRepository extends JpaRepository<BattingPerformance, Long> {
    List<BattingPerformance> findByInningsIdOrderByBattingPosition(Long inningsId);
    List<BattingPerformance> findByPlayerIdAndInnings_MatchResult_Match_Config_Id(Long playerId, Long configId);
}
