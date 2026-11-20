package com.manacommunity.api.communityops.controlroom;

public record OpsControlRoomSummary(
    long staffPresentToday,
    long pendingParcels,
    long visitorsToday,
    long overdueMaintenanceLogs,
    long upcomingMaintenanceIn7Days,
    long pendingMoveRequests
) {}
