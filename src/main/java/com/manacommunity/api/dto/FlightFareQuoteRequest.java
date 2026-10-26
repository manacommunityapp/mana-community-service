package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlightFareQuoteRequest {

    @NotBlank
    private String searchId;     // from FlightSearchResponse

    @NotBlank
    private String optionId;     // from FlightSearchResponse.FlightOption
}
