package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityEventCalendarAdapter implements CalendarAdapter {

    private final EventCommunityRepository eventRepository;

    @Override
    public EventSource getSource() {
        return EventSource.COMMUNITY_EVENT;
    }

    @Override
    public CalendarType getCalendarType() {
        return CalendarType.COMMUNITY_CALENDAR;
    }

    @Override
    public List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to) {
        if (communityId == null) {
            return List.of();
        }
        return eventRepository.findByCommunityIdOrderByStartDateDesc(communityId)
                .stream()
                .map(event -> toCalendarDto(event, from, to))
                .filter(Objects::nonNull)
                .toList();
    }

    private CalendarEventDto toCalendarDto(EventCommunity event, Instant from, Instant to) {
        if (event.getStartDate() == null) {
            return null;
        }

        LocalTime startTime = event.getStartTime() != null ? event.getStartTime() : LocalTime.of(9, 0);
        LocalTime endTime = event.getEndTime() != null ? event.getEndTime() : startTime.plusHours(2);
        LocalDateTime startDt = LocalDateTime.of(event.getStartDate(), startTime);
        LocalDateTime endDt = event.getEndDate() != null
                ? LocalDateTime.of(event.getEndDate(), endTime)
                : LocalDateTime.of(event.getStartDate(), endTime);

        Instant startInstant = startDt.toInstant(ZoneOffset.UTC);
        Instant endInstant = endDt.toInstant(ZoneOffset.UTC);

        // Filter within bounds
        if (endInstant.isBefore(from) || startInstant.isAfter(to)) {
            return null;
        }

        String color = event.getType() == EventCommunity.EventType.FESTIVAL ? "#D97706" : "#4F46E5";

        return new CalendarEventDto(
                "COMMUNITY_EVENT:" + event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation() != null ? event.getLocation() : event.getVenue(),
                startInstant,
                endInstant,
                event.getStartTime() == null,
                "UTC",
                CalendarType.COMMUNITY_CALENDAR,
                EventSource.COMMUNITY_EVENT,
                String.valueOf(event.getId()),
                color,
                false,
                false,
                "EVENT",
                String.valueOf(event.getId()),
                null
        );
    }
}
