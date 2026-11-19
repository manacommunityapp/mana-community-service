package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsBattingPerformance;
import com.manacommunity.api.sports.model.DismissalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsBattingPerformance b WHERE b.innings.id IN " +
           "(SELECT i.id FROM SportsMatchInnings i WHERE i.matchResult.id IN " +
           "(SELECT r.id FROM SportsMatchResult r WHERE r.match.config.id = :configId))")
    void deleteByConfigId(@Param("configId") Long configId);
}
