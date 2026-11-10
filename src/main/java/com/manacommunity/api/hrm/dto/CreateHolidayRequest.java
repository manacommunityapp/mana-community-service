package com.manacommunity.api.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateHolidayRequest {

    @NotBlank
    private String holidayName;

    @NotNull
    private LocalDate holidayDate;

    private Boolean isOptional;
}
