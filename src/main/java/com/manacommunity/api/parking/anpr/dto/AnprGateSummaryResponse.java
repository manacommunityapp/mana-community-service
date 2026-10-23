package com.manacommunity.api.parking.anpr.dto;

/** Summary stats for the security dashboard. */
public record AnprGateSummaryResponse(
        long totalEventsToday,
        long openCount,
        long holdCount,
        long denyCount,
        long pendingAlerts
) {}
