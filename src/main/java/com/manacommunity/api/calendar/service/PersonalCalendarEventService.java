package com.manacommunity.api.calendar.service;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.calendar.dto.PersonalEventRequest;
import com.manacommunity.api.calendar.entity.PersonalCalendarEvent;
import com.manacommunity.api.calendar.repository.CalendarReminderRepository;
import com.manacommunity.api.calendar.repository.PersonalCalendarEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PersonalCalendarEventService {

    private static final Map<String, String> CALENDAR_COLORS = Map.of(
            "MY_CALENDAR",          "#2563EB",
            "FAMILY_CALENDAR",      "#7C3AED",
            "COMMUNITY_CALENDAR",   "#059669",
            "SPORTS_CALENDAR",      "#D97706",
            "PAYMENT_DUES",         "#DC2626",
            "APPOINTMENT_CALENDAR", "#0891B2",
            "STAFF_SCHEDULE",       "#6B7280",
            "TRIP_EVENT_CALENDAR",  "#DB2777"
    );

    private final PersonalCalendarEventRepository repo;
    private final CalendarReminderRepository reminderRepo;
    private final RRuleExpander rruleExpander;

    @Transactional
    public CalendarEventDto create(PersonalEventRequest req, Long communityId, Long userId) {
        PersonalCalendarEvent event = PersonalCalendarEvent.builder()
                .communityId(communityId)
                .creatorUserId(userId)
                .calendarType(req.calendarType())
                .title(req.title())
                .description(req.description())
                .location(req.location())
                .startTime(Instant.parse(req.startTime()))
                .endTime(Instant.parse(req.endTime()))
                .allDay(req.allDay())
                .timezone(req.timezone() != null ? req.timezone() : "UTC")
                .rrule(req.rrule())
                .color(req.color())
                .build();
        if (req.rrule() != null) {
            event.setRruleUntil(computeRruleUntil(req.rrule(), event.getStartTime()));
        }
        return toDto(repo.save(event), userId);
    }

    @Transactional
    public CalendarEventDto update(Long id, PersonalEventRequest req, Long userId) {
        PersonalCalendarEvent event = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!event.getCreatorUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        event.setTitle(req.title());
        event.setDescription(req.description());
        event.setLocation(req.location());
        event.setStartTime(Instant.parse(req.startTime()));
        event.setEndTime(Instant.parse(req.endTime()));
        event.setAllDay(req.allDay());
        event.setTimezone(req.timezone() != null ? req.timezone() : "UTC");
        event.setRrule(req.rrule());
        event.setColor(req.color());
        event.setUpdatedAt(Instant.now());
        if (req.rrule() != null) {
            event.setRruleUntil(computeRruleUntil(req.rrule(), event.getStartTime()));
        }
        return toDto(repo.save(event), userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        PersonalCalendarEvent event = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!event.getCreatorUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        repo.delete(event);
    }

    private Instant computeRruleUntil(String rrule, Instant start) {
        if (rrule == null) return null;
        // Parse UNTIL from RRULE if present, else default 2 years out
        for (String part : rrule.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && "UNTIL".equalsIgnoreCase(kv[0].trim())) {
                try {
                    String s = kv[1].trim().replace("T", "").replace("Z", "");
                    int y = Integer.parseInt(s.substring(0, 4));
                    int m = Integer.parseInt(s.substring(4, 6));
                    int d = Integer.parseInt(s.substring(6, 8));
                    return java.time.LocalDate.of(y, m, d)
                            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
                } catch (Exception ignored) { /* fall through */ }
            }
        }
        return start.plus(java.time.Duration.ofDays(365 * 2));
    }

    CalendarEventDto toDto(PersonalCalendarEvent e, Long viewerUserId) {
        boolean hasReminder = reminderRepo.existsByUserIdAndEventSourceAndSourceId(
                viewerUserId, EventSource.PERSONAL.name(), String.valueOf(e.getId()));
        String color = e.getColor() != null ? e.getColor()
                : CALENDAR_COLORS.getOrDefault(e.getCalendarType(), "#2563EB");
        CalendarType ct = CalendarType.valueOf(e.getCalendarType());
        return new CalendarEventDto(
                "PERSONAL:" + e.getId(), e.getTitle(), e.getDescription(), e.getLocation(),
                e.getStartTime(), e.getEndTime(), e.isAllDay(), e.getTimezone(),
                ct, EventSource.PERSONAL,
                String.valueOf(e.getId()), color,
                e.isCancelled(), hasReminder, "PERSONAL_EVENT", String.valueOf(e.getId()), null
        );
    }
}
