package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class MobilityTripDto {
    private Long id;
    private String tripRef;
    private String serviceType;
    private String status;

    private GeoPoint pickup;
    private GeoPoint destination;

    private String pickupPin;
    private MobilityTripDriverDto driver;

    private BigDecimal baseFare;
    private BigDecimal distanceFare;
    private BigDecimal durationFare;
    private BigDecimal taxes;
    private BigDecimal quotedTotal;
    private BigDecimal finalFare;
    private double distanceKm;
    private int etaMinutes;

    private String paymentMethod;
    private Short passengerRating;
    private Instant requestedAt;
    private Instant completedAt;

    @Data @Builder
    public static class GeoPoint {
        private double lat;
        private double lng;
        private String address;
    }
}
