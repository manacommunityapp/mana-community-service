package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.booking.repository.ResourceBookingRepository;
import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ResourceBookingCalendarAdapter implements CalendarAdapter {

    private final ResourceBookingRepository resourceBookingRepository;

    @Override public EventSource getSource() { return EventSource.RESOURCE_BOOKING; }
    @Override public CalendarType getCalendarType() { return CalendarType.MY_CALENDAR; }

    @Override
    public List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to) {
        return resourceBookingRepository.findByBookedByIdOrderByBookingDateDescStartTimeDesc(userId)
                .stream()
                .filter(b -> {
                    if (b.getBookingDate() == null) return false;
                    Instant start = b.getBookingDate().atStartOfDay(ZoneOffset.UTC).toInstant();
                    return !start.isBefore(from) && start.isBefore(to);
                })
                .map(b -> {
                    Instant start = b.getBookingDate().atStartOfDay(ZoneOffset.UTC).toInstant();
                    Instant end = b.getStartTime() != null && b.getEndTime() != null
                            ? b.getBookingDate().atTime(b.getEndTime()).toInstant(ZoneOffset.UTC)
                            : start.plusSeconds(3600);
                    String resourceName = b.getResource() != null && b.getResource().getName() != null
                            ? b.getResource().getName() : "Facility";
                    return new CalendarEventDto(
                            "RESOURCE_BOOKING:" + b.getId(),
                            "Booking: " + resourceName,
                            null, null, start, end, false, "UTC",
                            CalendarType.MY_CALENDAR, EventSource.RESOURCE_BOOKING,
                            String.valueOf(b.getId()), "#2563EB", false, false,
                            "RESOURCE_BOOKING", String.valueOf(b.getId()), null
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
