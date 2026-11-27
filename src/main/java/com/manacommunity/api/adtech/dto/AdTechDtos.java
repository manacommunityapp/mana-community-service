package com.manacommunity.api.adtech.dto;

import java.util.List;
import java.util.Map;

public final class AdTechDtos {

    private AdTechDtos() {}

    public record CampaignTargetingDto(
            List<String> cities,
            List<String> flatTypes,
            List<String> segments,
            List<String> societyIds
    ) {}

    public record AdCreativeDto(
            String creativeId,
            String campaignId,
            String type,
            String assetUrl,
            String ctaUrl,
            String ctaText,
            int weight,
            Integer widthPx,
            Integer heightPx,
            String brandName,
            String brandLogoUrl,
            String headline,
            String bodyText
    ) {}

    public record AdCampaignDto(
            String campaignId,
            String brandId,
            String brandName,
            String name,
            long totalBudgetPaise,
            long spentPaise,
            long cpmPaise,
            String startAt,
            String endAt,
            String status,
            Long dailyCapPaise,
            int freqCapPerDay,
            CampaignTargetingDto targeting,
            List<AdCreativeDto> creatives
    ) {}

    public record AdRequestDto(
            String userId,
            String societyId,
            String city,
            String flatType,
            String placementId,
            Map<String, Object> appContext
    ) {}

    public record AdResponseDto(
            String impressionId,
            String campaignId,
            AdCreativeDto creative,
            String impressionTrackUrl,
            String clickTrackUrl,
            int freqCapRemaining,
            Long budgetPacingDelayMs
    ) {}

    public record ImpressionEventDto(
            String impressionId,
            String campaignId,
            String userId,
            String societyId,
            String placementId,
            String firedAt
    ) {}

    public record ClickEventDto(
            String impressionId,
            String campaignId
    ) {}

    public record BudgetPacingStateDto(
            String campaignId,
            long totalBudgetPaise,
            long spentPaise,
            long remainingPaise,
            double utilizationPct,
            boolean isHalted,
            String lastUpdatedAt
    ) {}
}
