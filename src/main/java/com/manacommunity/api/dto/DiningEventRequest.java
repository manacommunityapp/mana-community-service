package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DiningEventRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private LocalDate date;
    @NotNull
    private LocalTime time;
    private String venue;
    private Integer maxCapacity;
    private BigDecimal pricePerPerson;
    private String menuDescription;
}
