package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data @Builder
public class AirportRideRequestDto {
    private Long id;
    private String direction;
    private String airportCode;
    private String area;
    private Instant travelAt;
    private int timeWindowMinutes;
    private int seats;
    private int luggageCount;
    private String flightNumber;
    private String terminal;
    private String status;
    private Instant createdAt;
}
