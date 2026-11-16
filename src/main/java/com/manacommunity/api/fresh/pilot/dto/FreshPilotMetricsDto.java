package com.manacommunity.api.fresh.pilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshPilotMetricsDto {
    private int totalPilotVendors;
    private int approvedCapabilitiesCount;
    private int activeBatchesCount;
    private int releasedBatchesCount;
    private double coldChainCompliancePercent;
    private double morningDeliveryOnTimePercent;
    private int completedDropsCount;
    private int pendingDropsCount;
    private double rejectionRatePercent;
    private BigDecimal totalSettledPayouts;
    private int activePilotCommunitiesCount;
    private List<String> pilotCommunityNames;
}
