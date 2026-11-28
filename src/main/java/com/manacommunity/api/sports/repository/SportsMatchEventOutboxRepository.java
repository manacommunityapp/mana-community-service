package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.scheduler.SportsMatchEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsMatchEventOutboxRepository extends JpaRepository<SportsMatchEventOutbox, Long> {

    List<SportsMatchEventOutbox> findByMatchIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(Long matchId, Long sequenceNo);

    @Query("SELECT COALESCE(MAX(o.sequenceNo), 0) FROM SportsMatchEventOutbox o WHERE o.matchId = :matchId")
    Long findMaxSequenceNoByMatchId(@Param("matchId") Long matchId);

    List<SportsMatchEventOutbox> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
