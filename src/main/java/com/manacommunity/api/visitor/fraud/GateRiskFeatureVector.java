package com.manacommunity.api.visitor.fraud;

import lombok.Builder;

@Builder
public record GateRiskFeatureVector(
        String phoneNumber,
        String phoneHash,
        String vehicleNumber,
        Long communityId,
        String flatNumber,
        long societiesVisitedPastHour,
        long entriesSameFlatPast24Hours,
        int currentHour,
        boolean isNightEntry,
        double flatHistoricalNightVisitRatio,
        boolean isResidentVacationMode,
        boolean isConsortiumBlacklisted,
        boolean isVerifiedDeliveryPartner,
        long extractionLatencyMs,
        boolean isDegraded
) {
    public static GateRiskFeatureVector safeDegraded(String phone, Long communityId, String flatNumber, int hour) {
        return GateRiskFeatureVector.builder()
                .phoneNumber(phone)
                .phoneHash("")
                .vehicleNumber("")
                .communityId(communityId)
                .flatNumber(flatNumber)
                .societiesVisitedPastHour(1L)
                .entriesSameFlatPast24Hours(1L)
                .currentHour(hour)
                .isNightEntry(hour >= 0 && hour < 6)
                .flatHistoricalNightVisitRatio(0.0)
                .isResidentVacationMode(false)
                .isConsortiumBlacklisted(false)
                .isVerifiedDeliveryPartner(false)
                .extractionLatencyMs(0L)
                .isDegraded(true)
                .build();
    }
}
