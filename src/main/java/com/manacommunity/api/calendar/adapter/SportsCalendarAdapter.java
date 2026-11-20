package com.manacommunity.api.calendar.adapter;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.sports.model.SportsEventRegistration;
import com.manacommunity.api.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.sports.repository.SportsTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SportsCalendarAdapter implements CalendarAdapter {

    private final SportsEventRegistrationRepository registrationRepository;
    private final SportsTournamentRepository tournamentRepository;

    @Override public EventSource getSource() { return EventSource.SPORTS_EVENT; }
    @Override public CalendarType getCalendarType() { return CalendarType.SPORTS_CALENDAR; }

    @Override
    public List<CalendarEventDto> fetchEvents(Long communityId, Long userId, Instant from, Instant to) {
        return registrationRepository.findByUserId(userId)
                .stream()
                .filter(r -> r.getEvent() != null)
                .map(r -> {
                    var tournament = tournamentRepository.findByEventId(r.getEvent().getId()).orElse(null);
                    if (tournament == null || tournament.getCreatedAt() == null) return null;
                    Instant start = tournament.getCreatedAt().toInstant(java.time.ZoneOffset.UTC);
                    if (start.isBefore(from) || !start.isBefore(to)) return null;
                    return new CalendarEventDto(
                            "SPORTS_EVENT:" + r.getId(),
                            "Tournament: " + tournament.getName(),
                            null, null, start, start.plusSeconds(86400), false, "UTC",
                            CalendarType.SPORTS_CALENDAR, EventSource.SPORTS_EVENT,
                            String.valueOf(r.getEvent().getId()), "#D97706", false, false,
                            "SPORTS_TOURNAMENT", String.valueOf(tournament.getId()), null
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
