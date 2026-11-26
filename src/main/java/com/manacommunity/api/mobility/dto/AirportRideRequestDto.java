package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder
public class AirportRideRequestDto {
    private Long id;
    private String direction;
    private String journeyType;
    private String airportCode;
    private String hubCode;
    private String contextName;
    private String area;
    private Instant travelAt;
    private int timeWindowMinutes;
    private int seats;
    private int luggageCount;
    private String flightNumber;
    private String terminal;
    private String status;
    private Double pickupLat;
    private Double pickupLng;
    private Double destLat;
    private Double destLng;
    private Double maxDetourKm;
    private boolean allowCrossCommunity;
    private boolean autoReserve;
    private boolean parentConsentRequired;
    private String guardianName;
    private String guardianPhone;
    private String guardianConsentStatus;
    private Instant guardianConsentedAt;
    private Instant createdAt;
}
