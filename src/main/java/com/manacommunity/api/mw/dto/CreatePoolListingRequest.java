package com.manacommunity.api.mw.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreatePoolListingRequest {

    @NotBlank
    private String title;

    private String description;
    private String requiredSkills;

    @NotNull
    private LocalDate shiftDate;

    @NotNull
    private LocalTime shiftStart;

    @NotNull
    private LocalTime shiftEnd;

    private String location;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal payRate;

    private String payType;

    @Min(1)
    private Integer maxWorkers;
}
