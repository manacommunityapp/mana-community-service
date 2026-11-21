package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.homeservices.entity.HomeServiceBooking;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HomeServiceCalendarAdapter implements CalendarAdapter {

    private final EntityManager entityManager;

    @Override public EventSource getSource() { return EventSource.HOME_SERVICE_BOOKING; }
    @Override public CalendarType getCalendarType() { return CalendarType.STAFF_SCHEDULE; }

    @Override
    @SuppressWarnings("unchecked")
    public List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to) {
        List<HomeServiceBooking> bookings = entityManager.createQuery(
                "SELECT b FROM HomeServiceBooking b WHERE b.communityId = :communityId " +
                "AND b.residentUserId = :userId AND b.scheduledDate BETWEEN :from AND :to",
                HomeServiceBooking.class)
                .setParameter("communityId", communityId)
                .setParameter("userId", userId)
                .setParameter("from", from.atZone(ZoneOffset.UTC).toLocalDate())
                .setParameter("to", to.atZone(ZoneOffset.UTC).toLocalDate())
                .getResultList();

        return bookings.stream().map(b -> {
            if (b.getScheduledDate() == null) return null;
            Instant start = b.getScheduledDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = b.getScheduledTime() != null
                    ? b.getScheduledDate().atTime(b.getScheduledTime()).toInstant(ZoneOffset.UTC)
                    : start.plusSeconds(3600);
            return new CalendarEventDto(
                    "HOME_SERVICE:" + b.getId(),
                    "Service: " + (b.getDescription() != null ? b.getDescription() : "Home Service"),
                    b.getDescription(), null, start, end, false, "UTC",
                    CalendarType.STAFF_SCHEDULE, EventSource.HOME_SERVICE_BOOKING,
                    String.valueOf(b.getId()), "#6B7280", false, false,
                    "HOME_SERVICE", String.valueOf(b.getId()), null
            );
        }).filter(Objects::nonNull).toList();
    }
}
