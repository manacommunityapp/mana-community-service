package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchBallEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsMatchBallEventRepository extends JpaRepository<SportsMatchBallEvent, Long> {

    List<SportsMatchBallEvent> findByMatchIdAndIsUndoneFalseOrderByDeliveryNumber(Long matchId);

    List<SportsMatchBallEvent> findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(Long matchId, Integer inningsNumber);

    List<SportsMatchBallEvent> findByMatchIdOrderByDeliveryNumberDesc(Long matchId);
}
