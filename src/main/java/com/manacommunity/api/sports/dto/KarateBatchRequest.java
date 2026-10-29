package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class KarateBatchRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;
    private LocalTime classTime;

    @Size(max = 50)
    private String daysOfWeek;     // e.g. "MON,WED,FRI"

    private Integer maxCapacity;

    @Size(max = 200)
    private String venue;
}
