package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventBudgetRepository extends JpaRepository<EventBudget, Long> {

    Optional<EventBudget> findByEventId(Long eventId);

    List<EventBudget> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT b FROM EventBudget b JOIN FETCH b.lineItems WHERE b.event.id = :eventId")
    Optional<EventBudget> findByEventIdWithLineItems(@Param("eventId") Long eventId);

    boolean existsByEventId(Long eventId);
}
