package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.SportsMatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsMatchEventRepository extends JpaRepository<SportsMatchEvent, Long> {

    List<SportsMatchEvent> findByMatchIdAndIsUndoneFalseOrderByCreatedAt(Long matchId);

    List<SportsMatchEvent> findByMatchIdAndPeriodNumberAndIsUndoneFalse(Long matchId, Integer periodNumber);
}
