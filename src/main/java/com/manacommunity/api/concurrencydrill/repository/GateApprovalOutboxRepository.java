package com.manacommunity.api.concurrencydrill.repository;

import com.manacommunity.api.concurrencydrill.entity.GateApprovalOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GateApprovalOutboxRepository extends JpaRepository<GateApprovalOutboxEvent, Long> {
    Optional<GateApprovalOutboxEvent> findByIdempotencyKey(String idempotencyKey);
}
