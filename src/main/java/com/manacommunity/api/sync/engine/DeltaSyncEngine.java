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

    public record SyncDecision(SyncStatus status, Long serverChangeId, Long entityId, String reason) {}
}
