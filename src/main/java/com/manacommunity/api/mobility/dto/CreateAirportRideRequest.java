package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateAirportRideRequest {
    @NotBlank private String direction;
    @NotBlank @Size(min = 3, max = 3) private String airportCode;
    @NotBlank private String area;
    @NotBlank private String travelAt;
    @Min(15) @Max(180) private int timeWindowMinutes = 30;
    @Min(1) @Max(8) private int seats = 1;
    @Min(0) @Max(12) private int luggageCount = 0;
    private String flightNumber;
    private String terminal;
}
