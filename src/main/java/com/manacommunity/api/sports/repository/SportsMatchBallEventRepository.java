package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchBallEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SportsMatchBallEventRepository extends JpaRepository<SportsMatchBallEvent, Long> {

    List<SportsMatchBallEvent> findByMatchIdAndIsUndoneFalseOrderByDeliveryNumber(Long matchId);

    List<SportsMatchBallEvent> findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(Long matchId, Integer inningsNumber);

    List<SportsMatchBallEvent> findByMatchIdOrderByDeliveryNumberDesc(Long matchId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsMatchBallEvent b WHERE b.match.id IN " +
           "(SELECT m.id FROM SportsTournamentMatch m WHERE m.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
