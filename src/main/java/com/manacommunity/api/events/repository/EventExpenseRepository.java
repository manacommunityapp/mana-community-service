package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventExpenseRepository extends JpaRepository<EventExpense, Long> {

    List<EventExpense> findByEventIdOrderByExpenseDateDesc(Long eventId);

    List<EventExpense> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EventExpense e WHERE e.event.id = :eventId")
    double sumAmountByEvent(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EventExpense e WHERE e.community.id = :communityId")
    double sumAmountByCommunity(@Param("communityId") Long communityId);
}
