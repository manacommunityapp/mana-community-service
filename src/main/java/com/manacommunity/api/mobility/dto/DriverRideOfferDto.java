package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class DriverRideOfferDto {
    private String offerId;
    private Long tripId;
    private String passengerName;
    private String pickupAddress;
    private String destAddress;
    private BigDecimal fareAmount;
    private double distanceKm;
    private Instant expiresAt;
}
