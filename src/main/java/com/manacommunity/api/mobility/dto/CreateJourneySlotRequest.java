package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateJourneySlotRequest {
    @NotBlank private String journeyType;
    @NotBlank private String direction;
    @NotBlank private String hubCode;
    @NotBlank private String origin;
    @NotBlank private String destination;
    @NotBlank private String departureTime;
    @NotBlank private String daysOfWeek;
    @Min(1) @Max(30) private int seatCapacity = 4;
    @Min(0) @Max(30) private int luggageCapacity = 0;
    @DecimalMin("0") private BigDecimal pricePerSeat = BigDecimal.ZERO;
}
