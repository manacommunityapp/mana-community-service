package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface SportsMatchEventRepository extends JpaRepository<SportsMatchEvent, Long> {

    List<SportsMatchEvent> findByMatchIdAndIsUndoneFalseOrderByCreatedAt(Long matchId);

    List<SportsMatchEvent> findByMatchIdAndPeriodNumberAndIsUndoneFalse(Long matchId, Integer periodNumber);

    @Query("SELECT e FROM SportsMatchEvent e WHERE e.match.id IN :matchIds AND e.isUndone = false")
    List<SportsMatchEvent> findActiveEventsByMatchIds(@Param("matchIds") List<Long> matchIds);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM SportsMatchEvent e WHERE e.match.id IN " +
           "(SELECT m.id FROM SportsTournamentMatch m WHERE m.config.id = :configId)")
    void deleteByConfigId(@Param("configId") Long configId);
}
