package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CateringRequestDto {
    private String occasionType;
    @NotNull
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String venue;
    @NotNull
    private Integer guestCount;
    private BigDecimal budget;
    private String menuPreferences;
    private String dietaryRequirements;
}
