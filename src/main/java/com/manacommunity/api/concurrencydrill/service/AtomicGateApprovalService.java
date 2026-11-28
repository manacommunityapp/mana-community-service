package com.manacommunity.api.concurrencydrill.service;

import com.manacommunity.api.concurrencydrill.entity.AtomicGateApproval;
import com.manacommunity.api.concurrencydrill.entity.GateApprovalOutboxEvent;
import com.manacommunity.api.concurrencydrill.repository.AtomicGateApprovalRepository;
import com.manacommunity.api.concurrencydrill.repository.GateApprovalOutboxRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

/**
 * Concurrency-Safe Gate Approval Service:
 * Eliminates TOCTOU races and Kafka dual-write split-brain by combining:
 * 1) Idempotency key deduplication for 2G network retries,
 * 2) DB-level Compare-And-Set transition (WHERE status = 'PENDING'),
 * 3) Transactional Outbox event persisted atomically in the same DB commit.
 */
@Service
@RequiredArgsConstructor
public class AtomicGateApprovalService {

    private static final Set<String> TERMINAL_DECISIONS = Set.of("APPROVED", "DENIED");

    private final AtomicGateApprovalRepository approvalRepo;
    private final GateApprovalOutboxRepository outboxRepo;

    @Builder
    public record ApprovalDecisionResult(
            Long approvalId,
            String status,
            int version,
            boolean idempotentReplay,
            String outboxIdempotencyKey
    ) {}

    @Transactional
    public ApprovalDecisionResult decideApproval(
            Long approvalId,
            String decision,
            Long actorId,
            String idempotencyKey
    ) {
        if (approvalId == null || actorId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("approvalId, actorId, and idempotencyKey are required");
        }
        String normDecision = decision != null ? decision.trim().toUpperCase(Locale.ROOT) : "";
        if (!TERMINAL_DECISIONS.contains(normDecision)) {
            throw new IllegalArgumentException("Decision must be APPROVED or DENIED");
        }

        String cleanKey = idempotencyKey.trim();
        var priorOutbox = outboxRepo.findByIdempotencyKey(cleanKey);
        if (priorOutbox.isPresent()) {
            AtomicGateApproval existing = approvalRepo.findById(approvalId)
                    .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));
            return ApprovalDecisionResult.builder()
                    .approvalId(existing.getId())
                    .status(existing.getStatus())
                    .version(existing.getVersion())
                    .idempotentReplay(true)
                    .outboxIdempotencyKey(cleanKey)
                    .build();
        }

        Instant now = Instant.now();
        int updated = approvalRepo.transitionFromPending(approvalId, normDecision, actorId, now);
        AtomicGateApproval settled = approvalRepo.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        if (updated == 0) {
            if (normDecision.equals(settled.getStatus()) && actorId.equals(settled.getDecidedBy())) {
                return ApprovalDecisionResult.builder()
                        .approvalId(settled.getId())
                        .status(settled.getStatus())
                        .version(settled.getVersion())
                        .idempotentReplay(true)
                        .outboxIdempotencyKey(cleanKey)
                        .build();
            }
            throw new ApprovalConflictException(
                    "Approval " + approvalId + " already settled as " + settled.getStatus());
        }

        String payload = String.format(
                "{\"approvalId\":%d,\"communityId\":%d,\"flatId\":%d,\"status\":\"%s\",\"version\":%d,\"actorId\":%d}",
                settled.getId(), settled.getCommunityId(), settled.getFlatId(),
                settled.getStatus(), settled.getVersion(), actorId
        );

        outboxRepo.save(GateApprovalOutboxEvent.builder()
                .aggregateId(settled.getId())
                .eventType("GATE_APPROVAL_" + normDecision)
                .payloadJson(payload)
                .idempotencyKey(cleanKey)
                .build());

        return ApprovalDecisionResult.builder()
                .approvalId(settled.getId())
                .status(settled.getStatus())
                .version(settled.getVersion())
                .idempotentReplay(false)
                .outboxIdempotencyKey(cleanKey)
                .build();
    }

    public static class ApprovalConflictException extends RuntimeException {
        public ApprovalConflictException(String message) {
            super(message);
        }
    }
}
