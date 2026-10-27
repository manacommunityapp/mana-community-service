package com.manacommunity.api.mobility.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class JourneySlotDto {
    private Long id;
    private String journeyType;
    private String direction;
    private String hubCode;
    private String origin;
    private String destination;
    private String departureTime;
    private String daysOfWeek;
    private int seatCapacity;
    private int luggageCapacity;
    private BigDecimal pricePerSeat;
    private String status;
}
