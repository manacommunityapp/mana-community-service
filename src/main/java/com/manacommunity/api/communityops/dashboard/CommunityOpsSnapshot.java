package com.manacommunity.api.communityops.dashboard;

public record CommunityOpsSnapshot(
        Long communityId,
        long openComplaints,
        long overduePayments,
        long activeGuards,
        long todayVisitors,
        long staffPresent,
        long parkingOccupied,
        long parkingTotal,
        long parkingAvailable,
        java.time.Instant asOf
) {}
