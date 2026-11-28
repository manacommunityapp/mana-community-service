package com.manacommunity.api.water.anomaly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TankerDeliveryVerificationRequest {

    private Long deliveryId;
    private Long communityId;
    private String tankerRegistration;
    private String vendorName;
    private double invoicedCapacityLiters;
    private BigDecimal invoicedCost;
    private Long targetSumpTankId;
    private LocalDateTime gateEntryTime;
    private LocalDateTime decantingStartTime;
    private LocalDateTime decantingEndTime;
    private double sumpLevelBeforeLiters;
    private double sumpLevelAfterLiters;
    private Double flowMeterTotalizerLiters;
    private double pumpTransfersOutDuringDischargeLiters;
}
