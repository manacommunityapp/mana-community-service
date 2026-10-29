package com.manacommunity.api.mobility.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateJourneyRequest {
    @NotBlank private String journeyType;
    @NotBlank private String direction;
    @NotBlank private String hubCode;
    @NotBlank private String area;
    private String contextName;
    @NotBlank private String travelAt;
    @Min(15) @Max(180) private int timeWindowMinutes = 30;
    @Min(1) @Max(8) private int seats = 1;
    @Min(0) @Max(12) private int luggageCount = 0;
}
