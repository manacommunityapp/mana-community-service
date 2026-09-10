package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsBattingPerformance;
import com.manacommunity.api.model.scheduler.DismissalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SportsBattingPerformanceRepository extends JpaRepository<SportsBattingPerformance, Long> {
    List<SportsBattingPerformance> findByInningsIdOrderByBattingPosition(Long inningsId);
    List<SportsBattingPerformance> findByPlayerIdAndInnings_MatchResult_Match_Config_Id(Long playerId, Long configId);

    @Query("SELECT COUNT(b) FROM SportsBattingPerformance b WHERE b.fielder.id = :playerId "
         + "AND b.dismissalType = :dismissalType "
         + "AND b.innings.matchResult.match.config.id = :configId")
    long countByFielderIdAndDismissalTypeAndConfigId(
        @Param("playerId") Long playerId,
        @Param("dismissalType") DismissalType dismissalType,
        @Param("configId") Long configId);
}
