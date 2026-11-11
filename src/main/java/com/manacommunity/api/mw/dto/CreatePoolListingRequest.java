package com.manacommunity.api.mw.dto;

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
    private BigDecimal payRate;
    private String payType;
    private Integer maxWorkers;
}
