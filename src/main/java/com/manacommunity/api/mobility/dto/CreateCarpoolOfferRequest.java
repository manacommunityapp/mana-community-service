package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateCarpoolOfferRequest {
    @NotBlank private String origin;
    @NotBlank private String destination;
    @NotBlank private String departureTime;   // ISO-8601
    @Min(1) @Max(8) private int totalSeats = 3;
    @DecimalMin("0") private java.math.BigDecimal pricePerSeat;
    private String vehicleModel;
    private String vehicleNumber;
}
