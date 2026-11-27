package com.manacommunity.api.calendar.dto;

import jakarta.validation.constraints.NotBlank;

public record ReminderRequest(
        @NotBlank String eventSource,
        @NotBlank String sourceId,
        int minutesBefore,
        @NotBlank String eventStartTime
) {}
