package com.manacommunity.api.guard.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record GuardShiftRequest(
        @NotNull Long guardId,
        @NotNull LocalDate shiftDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String gate,
        String notes
) {}
