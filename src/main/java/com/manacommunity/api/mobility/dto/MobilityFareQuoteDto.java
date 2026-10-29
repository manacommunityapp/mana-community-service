package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class MobilityFareQuoteDto {
    private String quoteId;
    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal durationFare;
    private BigDecimal taxes;
    private BigDecimal total;
    private double distanceKm;
    private int etaMinutes;
    private long expiresAt;  // epoch-ms: client shows countdown
}
