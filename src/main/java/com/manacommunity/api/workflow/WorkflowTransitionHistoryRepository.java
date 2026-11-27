package com.manacommunity.api.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowTransitionHistoryRepository extends JpaRepository<WorkflowTransitionHistory, Long> {

    List<WorkflowTransitionHistory> findByEntityTypeAndEntityIdOrderByTransitionedAtDesc(
            String entityType, String entityId);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
