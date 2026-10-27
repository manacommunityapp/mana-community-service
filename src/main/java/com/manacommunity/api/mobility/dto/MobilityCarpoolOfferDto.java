package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder
public class MobilityCarpoolOfferDto {
    private Long id;
    private String driverName;
    private BigDecimal rating;
    private String origin;
    private String destination;
    private String journeyType;
    private String journeyDirection;
    private String hubCode;
    private String contextName;
    private String airportDirection;
    private String airportCode;
    private String airportName;
    private String flightNumber;
    private String terminal;
    private int luggageCapacity;
    private Instant departureTime;
    private int totalSeats;
    private int availableSeats;
    private BigDecimal pricePerSeat;
    private String vehicleModel;
    private String vehicleNumber;
    private String status;
}
