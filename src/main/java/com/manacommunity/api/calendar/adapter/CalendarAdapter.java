package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import java.time.Instant;
import java.util.List;

public interface CalendarAdapter {
    EventSource getSource();
    CalendarType getCalendarType();
    List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to);
}
