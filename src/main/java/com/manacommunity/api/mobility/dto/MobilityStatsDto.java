package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class MobilityStatsDto {
    private long totalTrips;
    private long completedTrips;
    private long cancelledTrips;
    private BigDecimal totalSpend;
    private double avgRating;
}
