package com.manacommunity.api.finops.dto;

import java.util.List;

public final class FinOpsDtos {

    private FinOpsDtos() {}

    public record EscalationStepDto(
            String channel,
            int costPaise,
            int triggerAtMs,
            int timeoutMs
    ) {}

    public record DispatchAlertRequest(
            long societyId,
            String residentId,
            String visitorType,
            int slaMs,
            boolean wsOnlineOverride,
            boolean aggressiveDozeOverride,
            Boolean waOptInOverride,
            boolean forceExplorationProbe
    ) {}

    public record DeliveryRoutePlanDto(
            String alertId,
            long societyId,
            String residentId,
            int slaMs,
            String initialChannel,
            String currentChannel,
            int currentCostPaise,
            int baselineIvrCostPaise,
            int projectedSavingsPaise,
            String status,
            String routingReason,
            boolean explorationProbe,
            List<EscalationStepDto> escalationPath
    ) {}

    public record AckAlertRequest(
            String channel,
            int latencyMs
    ) {}

    public record DeliveryEventDto(
            String alertId,
            long societyId,
            String residentId,
            String visitorType,
            String initialChannel,
            String currentChannel,
            String winningChannel,
            Integer resolvedLatencyMs,
            Boolean slaMet,
            int totalCostPaise,
            int savingsPaise,
            String status,
            String routingReason,
            String createdAt
    ) {}

    public record TelecomFinOpsSummaryDto(
            long societyId,
            long totalAlerts,
            long resolvedCount,
            double slaCompliancePct,
            double avgLatencyMs,
            long actualSpendPaise,
            long baselineIvrSpendPaise,
            long totalSavedPaise
    ) {}

    public record StorageTierPolicyDto(
            String assetClass,
            String targetFormat,
            String hotTier,
            int hotDays,
            String warmTier,
            int warmDays,
            String archiveTier,
            int retentionDays,
            int minBillableKb,
            String notes
    ) {}

    public record RecordCompressionAuditRequest(
            long societyId,
            String assetClass,
            long originalBytes,
            long compressedBytes,
            String format
    ) {}

    public record StorageFinOpsSummaryDto(
            long societyId,
            long totalObjectsTracked,
            long totalOriginalBytes,
            long totalCompressedBytes,
            double compressionSavingsPct,
            long estimatedMonthlyS3SavedPaise,
            List<StorageTierPolicyDto> policies
    ) {}
}
