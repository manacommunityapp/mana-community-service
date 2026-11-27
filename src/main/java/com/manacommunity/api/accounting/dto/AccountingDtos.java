package com.manacommunity.api.accounting.dto;

import java.util.List;
import java.util.Map;

public final class AccountingDtos {

    private AccountingDtos() {}

    public record TaxRuleVersionDto(
            String ruleVersionId,
            String ruleFamily,
            String fyCode,
            int versionNo,
            String effectiveFrom,
            String effectiveTo,
            String gazetteRef,
            Map<String, Object> ruleSpec,
            String specSha256
    ) {}

    public record PublishTaxRuleRequest(
            String ruleFamily,
            String fyCode,
            String effectiveFrom,
            String gazetteRef,
            Map<String, Object> ruleSpec
    ) {}

    public record MaintenanceChargeLine(
            String code,
            String label,
            long amountPaise,
            boolean pureAgentPassThrough
    ) {}

    public record MaintenanceGstRequest(
            long societyId,
            String fyCode,
            String supplyDate,
            String flatNumber,
            String voucherNumber,
            boolean societyGstRegistered,
            long societyTurnoverPaise,
            List<MaintenanceChargeLine> lines,
            boolean persistSnapshot
    ) {}

    public record MaintenanceGstResult(
            String ruleVersionId,
            String fyCode,
            String supplyDate,
            long totalBilledPaise,
            long maintenanceBasePaise,
            long pureAgentExcludedPaise,
            boolean gstApplicable,
            String reason,
            int appliedRateBps,
            long cgstPaise,
            long sgstPaise,
            long totalGstPaise,
            long grandTotalPaise,
            String snapshotSha256
    ) {}

    public record VendorTdsRequest(
            long societyId,
            String fyCode,
            String creditOrPaymentDate,
            String voucherNumber,
            String vendorPan,
            String panStatusOverride,
            String tdsSection,
            long grossAmountPaise,
            boolean persistSnapshot
    ) {}

    public record PanValidationResult(
            String pan,
            boolean formatValid,
            String entityTypeChar,
            String entityCategory,
            String status,
            boolean penal206aaTriggered
    ) {}

    public record VendorTdsResult(
            String ruleVersionId,
            String tdsSection,
            PanValidationResult panValidation,
            long grossAmountPaise,
            long priorCumulativeGrossPaise,
            long newCumulativeGrossPaise,
            boolean thresholdCrossed,
            boolean catchUpApplied,
            int appliedRateBps,
            long tdsDeductedPaise,
            long netPayablePaise,
            String reason
    ) {}

    public record RecalculateTaxRequest(
            long societyId,
            String fyCode,
            String ruleFamily,
            String targetRuleVersionId,
            String fromDate,
            String toDate
    ) {}

    public record TaxAdjustmentNoteDto(
            String noteId,
            String originalVoucherId,
            String noteType,
            String noteNumber,
            long deltaPaise,
            String ruleVersionId
    ) {}

    public record RecalculationBatchDto(
            String batchId,
            long societyId,
            String fyCode,
            String ruleFamily,
            String targetRuleVersionId,
            int evaluatedCount,
            int adjustedCount,
            long netDeltaPaise,
            List<TaxAdjustmentNoteDto> notes
    ) {}

    public record LedgerLineDto(
            String ledgerCode,
            String ledgerName,
            long debitPaise,
            long creditPaise,
            String costCategory,
            String costCenter
    ) {}

    public record PostAuditEntryRequest(
            long societyId,
            String voucherId,
            String voucherNumber,
            String voucherType,
            String postingDate,
            String sourceSystem,
            String reversesEntryId,
            String taxSnapshotSha256,
            List<LedgerLineDto> lines
    ) {}

    public record AuditLedgerEntryDto(
            String entryId,
            long societyId,
            long seqNo,
            String voucherId,
            String voucherNumber,
            String voucherType,
            String postingDate,
            String sourceSystem,
            String reversesEntryId,
            List<LedgerLineDto> lines,
            String taxSnapshotSha256,
            String prevEntryHash,
            String entryHash,
            String createdAt
    ) {}

    public record AuditChainVerificationDto(
            long societyId,
            int totalEntries,
            boolean chainValid,
            String merkleRoot,
            Long brokenAtSeqNo,
            String message
    ) {}

    public record TallyOutboxItemDto(
            String voucherId,
            String tallyGuid,
            String voucherNumber,
            String cloudEntryHash,
            String syncedPayloadHash,
            String syncStatus,
            String tallyXml
    ) {}

    public record TallyAckRequest(
            long societyId,
            String voucherId,
            String tallyGuid,
            long tallyAlterId
    ) {}

    public record TallyInboundVoucherDto(
            String tallyGuid,
            String voucherNumber,
            String voucherType,
            String postingDate,
            long tallyAlterId,
            String narration,
            List<LedgerLineDto> lines
    ) {}

    public record TallyInboundSyncRequest(
            long societyId,
            List<TallyInboundVoucherDto> vouchers
    ) {}

    public record TallySyncConflictDto(
            String conflictId,
            long societyId,
            String voucherId,
            String tallyGuid,
            Map<String, Object> cloudSnapshot,
            Map<String, Object> tallySnapshot,
            String resolutionStatus,
            String createdAt
    ) {}

    public record ResolveConflictRequest(
            long societyId,
            String resolutionAction
    ) {}

    public record TallySyncSummaryDto(
            long societyId,
            long pendingOutboxCount,
            long syncedCount,
            long openConflictsCount
    ) {}
}
