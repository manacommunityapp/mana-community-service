package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventExpenseRepository extends JpaRepository<EventExpense, Long> {
    List<EventExpense> findByEventIdOrderByCreatedAtDesc(Long eventId);
}
