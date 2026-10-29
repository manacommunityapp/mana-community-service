package com.manacommunity.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FlightSearchRequest {

    @NotBlank
    @Size(min = 3, max = 3, message = "Origin must be a 3-letter IATA code")
    private String origin;           // e.g. "BLR"

    @NotBlank
    @Size(min = 3, max = 3, message = "Destination must be a 3-letter IATA code")
    private String destination;      // e.g. "GOI"

    @NotBlank
    private String departureDate;    // ISO date "2026-11-15"

    @Min(1) @Max(9)
    private int adults = 1;

    @Min(0) @Max(6)
    private int children = 0;

    @Min(0) @Max(4)
    private int infants = 0;

    private String cabinClass = "ECONOMY";  // ECONOMY | BUSINESS | FIRST
    private boolean directOnly = false;
}
