package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsMatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface SportsMatchEventRepository extends JpaRepository<SportsMatchEvent, Long> {

    List<SportsMatchEvent> findByMatchIdAndIsUndoneFalseOrderByCreatedAt(Long matchId);

    List<SportsMatchEvent> findByMatchIdAndPeriodNumberAndIsUndoneFalse(Long matchId, Integer periodNumber);

    @Query("SELECT e FROM SportsMatchEvent e WHERE e.match.id IN :matchIds AND e.isUndone = false")
    List<SportsMatchEvent> findActiveEventsByMatchIds(@Param("matchIds") List<Long> matchIds);
}
