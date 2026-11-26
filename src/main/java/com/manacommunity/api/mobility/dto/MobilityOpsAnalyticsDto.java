package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data @Builder
public class MobilityOpsAnalyticsDto {
    private long totalTrips;
    private long activeTrips;
    private long completedTrips;
    private long cancelledTrips;
    private double cancellationRatePercent;

    private BigDecimal grossRevenue;
    private BigDecimal cashRevenue;
    private BigDecimal onlineRevenue;
    private BigDecimal platformFeeTotal;
    private BigDecimal pendingSettlementTotal;
    private BigDecimal settledTotal;
    private BigDecimal refundedTotal;

    private long totalDrivers;
    private long approvedDrivers;
    private long onlineDrivers;
    private double avgDriverRating;
    private double completedWithinSlaPercent;

    private long activeCarpoolOffers;
    private long carpoolSeatsTotal;
    private long carpoolSeatsBooked;
    private double carpoolUtilizationPercent;
}
