package com.manacommunity.api.homeservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class AdminAnalyticsDto {
    private long totalWorkers;
    private long verifiedWorkers;
    private long pendingVerifications;
    private long activeBookings;
    private long openRequirements;
    private long activeIncidents;
    private Map<String, Long> workersByCategory;
}
