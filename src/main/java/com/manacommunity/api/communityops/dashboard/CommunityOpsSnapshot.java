package com.manacommunity.api.communityops.dashboard;

public record CommunityOpsSnapshot(
        Long communityId,
        long openComplaints,
        long overduePaymentsCount,
        java.math.BigDecimal overduePaymentsAmount,
        long activeGuards,
        long totalGuards,
        long todayVisitors,
        long staffPresent,
        long parkingOccupied,
        long parkingTotal,
        long parkingAvailable,
        java.time.Instant asOf
) {}
