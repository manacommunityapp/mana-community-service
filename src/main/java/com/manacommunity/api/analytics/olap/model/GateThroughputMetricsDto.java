package com.manacommunity.api.analytics.olap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Aggregated OLAP metrics computed over high-throughput gate passage streams
 * without querying transactional PostgreSQL tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateThroughputMetricsDto {

    private Long communityId;
    private String gateId;
    private LocalDate eventDate;
    private long totalPassages;
    private long preAuthorizedCount;
    private double avgApprovalSeconds;
    private double p95ApprovalSeconds;
    private long deliveryPassages;
    private long cabPassages;
    private long guestPassages;
    private long dailyHelpPassages;
    private Map<Integer, Long> hourlyDistribution;
    private Map<Integer, Long> hourlyDeliveryDistribution;
    private int peakDeliveryHour;
    private long peakDeliveryHourVolume;
    private boolean slaBreached; // e.g. avg approval time > 120s
}
