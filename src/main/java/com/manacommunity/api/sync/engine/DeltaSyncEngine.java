package com.manacommunity.api.sync.engine;

import com.manacommunity.api.sync.SyncChangeLog;
import com.manacommunity.api.sync.SyncEnums.*;
import com.manacommunity.api.sync.dto.SyncDtos.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class DeltaSyncEngine {

    /**
     * Resolves conflict between incoming offline client mutation and server state.
     *
     * Rules:
     * 1. Idempotency Check: If clientMutationId already recorded, return previous status.
     * 2. If entity does not exist on server yet -> APPLIED.
     * 3. If server record was modified after client mutation timestamp and strategy is SERVER_WINS -> CONFLICT_SERVER_WINS.
     * 4. Otherwise -> APPLIED.
     */
    public SyncDecision evaluateMutation(
            ClientMutationItem mutation,
            Optional<SyncChangeLog> existingMutation,
            Optional<LocalDateTime> lastServerEntityUpdate,
            ConflictResolutionStrategy strategy) {

        // 1. Idempotent deduplication
        if (existingMutation.isPresent()) {
            SyncChangeLog log = existingMutation.get();
            return new SyncDecision(log.getStatus(), log.getId(), log.getEntityId(), "Idempotent repeat mutation acknowledged");
        }

        // 2. Conflict check
        if (lastServerEntityUpdate.isPresent() && strategy == ConflictResolutionStrategy.SERVER_WINS) {
            LocalDateTime serverMod = lastServerEntityUpdate.get();
            if (serverMod.isAfter(mutation.getClientTimestamp())) {
                return new SyncDecision(
                        SyncStatus.CONFLICT_SERVER_WINS,
                        null,
                        mutation.getEntityId(),
                        "Server has newer modifications (" + serverMod + " > " + mutation.getClientTimestamp() + ")"
                );
            }
        }

        return new SyncDecision(SyncStatus.APPLIED, null, mutation.getEntityId(), "Successfully applied offline mutation");
    }

    public GateSyncDecision evaluateGateMutation(
            ClientMutationItem mutation,
            Optional<SyncChangeLog> existingMutation,
            Long lastCommittedSeq,
            boolean nonceAlreadyConsumed,
            boolean signatureValid,
            boolean deviceActive) {
        if (existingMutation.isPresent()) {
            SyncChangeLog existing = existingMutation.get();
            return new GateSyncDecision(
                    existing.getStatus(),
                    existing.getId(),
                    existing.getEntityId(),
                    existing.isOfflineOverride(),
                    existing.getConflictReason(),
                    "Idempotent repeat mutation acknowledged",
                    true
            );
        }
        if (!deviceActive) {
            return new GateSyncDecision(
                    SyncStatus.REJECTED_REPLAY,
                    null,
                    mutation.getEntityId(),
                    false,
                    "DEVICE_REVOKED_OR_SUSPENDED",
                    "Gate device is suspended or revoked",
                    false
            );
        }
        if (!signatureValid) {
            return new GateSyncDecision(
                    SyncStatus.REJECTED_REPLAY,
                    null,
                    mutation.getEntityId(),
                    false,
                    "INVALID_DEVICE_SIGNATURE",
                    "Offline payload signature failed verification",
                    false
            );
        }
        if (mutation.getLocalSeq() != null && lastCommittedSeq != null && mutation.getLocalSeq() <= lastCommittedSeq) {
            return new GateSyncDecision(
                    SyncStatus.REJECTED_REPLAY,
                    null,
                    mutation.getEntityId(),
                    false,
                    "STALE_SEQUENCE_REPLAY",
                    "Sequence " + mutation.getLocalSeq() + " <= committed checkpoint " + lastCommittedSeq,
                    false
            );
        }
        if (nonceAlreadyConsumed) {
            return new GateSyncDecision(
                    SyncStatus.REJECTED_REPLAY,
                    null,
                    mutation.getEntityId(),
                    false,
                    "DUPLICATE_PASS_NONCE_REPLAY",
                    "Pass nonce already consumed in community",
                    false
            );
        }
        return new GateSyncDecision(SyncStatus.APPLIED, null, mutation.getEntityId(), false, null, "Gate check passed", false);
    }

    public record SyncDecision(SyncStatus status, Long serverChangeId, Long entityId, String reason) {}

    public record GateSyncDecision(
            SyncStatus status,
            Long serverChangeId,
            Long entityId,
            boolean offlineOverride,
            String conflictReason,
            String message,
            boolean idempotentRepeat) {}
}
