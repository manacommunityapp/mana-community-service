package com.manacommunity.api.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PersonalEventRequest(
        @NotBlank String title,
        String description,
        String location,
        @NotNull String startTime,
        @NotNull String endTime,
        boolean allDay,
        String timezone,
        @NotBlank String calendarType,
        String rrule,
        String color
) {}
