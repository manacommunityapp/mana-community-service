package com.manacommunity.api.recovery.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RecoveryDtos {

    private RecoveryDtos() {}

    public enum BackfillJobStatus {
        DRY_RUN,
        RUNNING,
        PAUSED_REPLICA_LAG,
        COMPLETED
    }

    public enum BackfillSettlementMode {
        ROLL_TO_NEXT_CYCLE,
        IMMEDIATE_DEBIT_NOTE
    }

    public enum GateAccessDecision {
        ALLOW_ACTIVE,
        PROVISIONAL_EMERGENCY_PASS,
        DENY_INACTIVE,
        DENY_BLACKLISTED
    }

    public enum GuardrailDecision {
        BLOCKED_ROW_CAP,
        BLOCKED_MISSING_CHECKER,
        CANARY_ABORTED,
        APPROVED,
        RESTORED_FROM_PITR
    }

    public record BackfillCandidateInput(
            long societyId,
            long flatId,
            String unitCode,
            UUID originalVoucherId,
            long billedGstPaise,
            long expectedGstPaise
    ) {}

    public record StageDryRunRequest(
            String incidentRef,
            BackfillSettlementMode settlementMode,
            List<BackfillCandidateInput> candidates
    ) {}

    public record ExecuteAdaptiveChunkRequest(
            UUID jobId,
            int replicaLagMs,
            int primaryCpuPct,
            int lockWaitCount
    ) {}

    public record GstBackfillItemDto(
            UUID itemId,
            UUID jobId,
            long societyId,
            long flatId,
            String unitCode,
            UUID originalVoucherId,
            long billedGstPaise,
            long expectedGstPaise,
            long deltaPaise,
            String debitNoteNumber,
            String idempotencyKey,
            String status
    ) {}

    public record GstBackfillJobDto(
            UUID jobId,
            String incidentRef,
            BackfillJobStatus status,
            BackfillSettlementMode settlementMode,
            int affectedSocieties,
            int affectedFlats,
            int processedItems,
            long totalDeltaPaise,
            int currentBatchSize,
            int lastReplicaLagMs,
            int sleepIntervalMs,
            int treasurerNotifiedCount,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record AdaptiveChunkResultDto(
            GstBackfillJobDto job,
            int itemsProcessedInChunk,
            String aimdAction
    ) {}

    public record RfidTagDto(
            String tagId,
            long societyId,
            String vehicleNumber,
            String flatUnitCode,
            String tagStatus,
            String preIncidentStatus,
            boolean blacklisted,
            String updatedBy,
            Instant updatedAt
    ) {}

    public record EmergencyOverrideRequest(
            long societyId,
            boolean enabled,
            Instant deactivatedSince,
            String activatedBy
    ) {}

    public record EmergencyOverrideDto(
            UUID overrideId,
            long societyId,
            String featureKey,
            boolean enabled,
            Instant deactivatedSince,
            int maxRowsGuardrail,
            int canaryAbortThresholdPct,
            String activatedBy,
            Instant activatedAt
    ) {}

    public record GateEvaluationDto(
            String tagId,
            long societyId,
            GateAccessDecision decision,
            boolean emergencyOverrideApplied,
            String message
    ) {}

    public record SurgicalPitrRestoreResultDto(
            long societyId,
            int restoredCount,
            int skippedBlacklistedOrUntouched,
            boolean overrideAutoCleared,
            String auditDecision
    ) {}

    public record BulkMutationCheckRequest(
            long societyId,
            String operationName,
            int requestedRows,
            int canaryRows,
            int canaryGateDropPct,
            long makerId,
            Long checkerId,
            String reason
    ) {}

    public record BulkMutationGuardrailDto(
            UUID guardId,
            long societyId,
            String operationName,
            int requestedRows,
            int canaryRows,
            int gateDropPct,
            long makerId,
            Long checkerId,
            GuardrailDecision decision,
            String reason,
            Instant createdAt
    ) {}
}
