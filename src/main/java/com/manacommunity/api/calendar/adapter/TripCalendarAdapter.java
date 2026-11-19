package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.trip.repository.TripBookingRepository;
import com.manacommunity.api.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TripCalendarAdapter implements CalendarAdapter {

    private final TripBookingRepository tripBookingRepository;
    private final TripRepository tripRepository;

    @Override public EventSource getSource() { return EventSource.TRIP; }
    @Override public CalendarType getCalendarType() { return CalendarType.TRIP_EVENT_CALENDAR; }

    @Override
    public List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to) {
        return tripBookingRepository.findByUserIdOrderByBookedAtDesc(userId)
                .stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .map(b -> tripRepository.findById(b.getTripId()).map(trip -> {
                    try {
                        LocalDate depDate = LocalDate.parse(trip.getDepartureDate());
                        Instant start = depDate.atStartOfDay(ZoneOffset.UTC).toInstant();
                        if (start.isBefore(from) || !start.isBefore(to)) return null;
                        return new CalendarEventDto(
                                "TRIP:" + b.getId(),
                                "Trip: " + trip.getTitle(),
                                null, trip.getDeparturePoint(),
                                start, start.plusSeconds(86400), true, "UTC",
                                CalendarType.TRIP_EVENT_CALENDAR, EventSource.TRIP,
                                String.valueOf(b.getId()), "#DB2777", false, false,
                                "TRIP", String.valueOf(trip.getId()), null
                        );
                    } catch (Exception e) {
                        return null;
                    }
                }).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }
}
