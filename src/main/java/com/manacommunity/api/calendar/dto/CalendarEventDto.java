package com.manacommunity.api.calendar.dto;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import java.time.Instant;
import java.util.Map;

public record CalendarEventDto(
        String id,
        String title,
        String description,
        String location,
        Instant startTime,
        Instant endTime,
        boolean allDay,
        String timezone,
        CalendarType calendarType,
        EventSource eventSource,
        String sourceId,
        String color,
        boolean isCancelled,
        boolean hasReminder,
        String deepLinkType,
        String deepLinkId,
        Map<String, Object> metadata
) {}
