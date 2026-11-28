package com.manacommunity.api.forensics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ForensicsDtos {

    private ForensicsDtos() {}

    public enum LedgerEventType {
        MAINTENANCE_INVOICE,
        PAYMENT_RECEIVED,
        CHEQUE_CLEARED,
        CHEQUE_BOUNCED_NSF,
        CREDIT_NOTE_BACKDATED,
        PENALTY_LEVIED,
        PENALTY_WAIVED
    }

    public enum AuditRiskTier {
        STANDARD,
        HIGH,
        CRITICAL
    }

    public enum SensitiveActionType {
        PENALTY_WAIVER,
        VENDOR_BLACKLIST,
        VISITOR_SOFT_DELETED,
        GUARD_SHIFT_OVERRIDE,
        CREDIT_NOTE_BACKDATED
    }

    public record AppendLedgerEventRequest(
            long societyId,
            long flatId,
            String unitCode,
            LedgerEventType eventType,
            LocalDate effectiveDate,
            Instant recordedAt,
            long signedAmountPaise,
            UUID causesEventId,
            String reasonCode,
            String actorName,
            String idempotencyKey,
            String linesJson
    ) {}

    public record BitemporalLedgerEventDto(
            UUID eventId,
            long societyId,
            long flatId,
            String unitCode,
            long streamVersion,
            long globalSeq,
            LedgerEventType eventType,
            LocalDate effectiveDate,
            Instant recordedAt,
            long signedAmountPaise,
            UUID causesEventId,
            String reasonCode,
            String actorName,
            String idempotencyKey,
            String linesJson,
            String prevEventHash,
            String eventHash
    ) {}

    public record LedgerCheckpointDto(
            UUID checkpointId,
            long societyId,
            long flatId,
            String unitCode,
            LocalDate checkpointDate,
            Instant recordedCutoff,
            long streamVersion,
            long balancePaise,
            String stateHash,
            Instant createdAt
    ) {}

    public record TimeTravelQueryRequest(
            long societyId,
            long flatId,
            LocalDate asOfEffectiveDate,
            Instant asOfRecordedAt
    ) {}

    public record TimeTravelReconstructionDto(
            long societyId,
            long flatId,
            String unitCode,
            LocalDate asOfEffectiveDate,
            Instant asOfRecordedAt,
            long asKnownBalancePaise,
            long restatedBalancePaise,
            long liveCurrentBalancePaise,
            long retroactiveVariancePaise,
            LedgerCheckpointDto checkpointUsed,
            int eventsReplayedFromCheckpoint,
            List<BitemporalLedgerEventDto> asKnownTimeline,
            List<BitemporalLedgerEventDto> retroactiveAdjustments
    ) {}

    public record RecordSensitiveAuditRequest(
            long societyId,
            SensitiveActionType actionType,
            String resourceType,
            String resourceId,
            long actorId,
            String actorName,
            Long checkerId,
            String checkerName,
            Long monetaryImpactPaise,
            String reason,
            String oldValueJson,
            String newValueJson,
            boolean tombstoneDelete,
            String ipAddress,
            String correlationId
    ) {}

    public record WormAuditEventDto(
            UUID auditId,
            long societyId,
            long seqNo,
            SensitiveActionType actionType,
            AuditRiskTier riskTier,
            String resourceType,
            String resourceId,
            long actorId,
            String actorName,
            Long checkerId,
            String checkerName,
            String reason,
            String oldValueJson,
            String newValueJson,
            boolean tombstoned,
            String ipAddress,
            String correlationId,
            String prevHash,
            String recordHash,
            String ecdsaSignature,
            Instant recordedAt
    ) {}

    public record ExternalMerkleAnchorDto(
            UUID anchorId,
            long societyId,
            long seqFrom,
            long seqTo,
            int eventCount,
            String headHash,
            String merkleRoot,
            String ecdsaSignature,
            String wormBucketUri,
            String objectLockMode,
            Instant retentionUntil,
            Instant anchoredAt
    ) {}

    public record ForensicVerificationReportDto(
            long societyId,
            int totalLocalEvents,
            int totalExternalAnchors,
            boolean chainValid,
            boolean signaturesValid,
            boolean externalAnchorMatch,
            boolean tailTruncationDetected,
            Long firstBrokenSeqNo,
            String failureReason,
            String latestLocalHeadHash,
            String latestAnchoredMerkleRoot,
            Instant verifiedAt
    ) {}

    public record SimulateDbaTamperRequest(
            long societyId,
            String mode,
            Long targetSeqNo,
            String forgedNewValueJson
    ) {}
}
