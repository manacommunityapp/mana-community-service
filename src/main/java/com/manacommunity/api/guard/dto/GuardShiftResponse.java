package com.manacommunity.api.guard.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record GuardShiftResponse(
        Long id,
        Long guardId,
        String guardName,
        LocalDate shiftDate,
        LocalTime startTime,
        LocalTime endTime,
        String gate,
        String status,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        String notes,
        LocalDateTime createdAt
) {}
