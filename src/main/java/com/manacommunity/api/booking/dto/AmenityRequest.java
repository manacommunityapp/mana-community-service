package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AmenityRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String type;

    private Integer maxCapacity;
    private String openTime;
    private String closeTime;
    private Integer slotDurationMinutes;
}
