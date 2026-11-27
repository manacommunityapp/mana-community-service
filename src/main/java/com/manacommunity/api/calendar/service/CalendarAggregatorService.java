package com.manacommunity.api.calendar.service;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.EventSource;
import com.manacommunity.api.calendar.adapter.CalendarAdapter;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.calendar.entity.FamilyCalendarMember;
import com.manacommunity.api.calendar.entity.PersonalCalendarEvent;
import com.manacommunity.api.calendar.repository.CalendarReminderRepository;
import com.manacommunity.api.calendar.repository.FamilyCalendarMemberRepository;
import com.manacommunity.api.calendar.repository.PersonalCalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarAggregatorService {

    private final List<CalendarAdapter> adapters;
    private final PersonalCalendarEventRepository personalRepo;
    private final CalendarReminderRepository reminderRepo;
    private final FamilyCalendarMemberRepository familyRepo;
    private final RRuleExpander rruleExpander;
    private final PersonalCalendarEventService personalService;

    public List<CalendarEventDto> getEvents(Long communityId, Long userId,
            Instant from, Instant to, List<CalendarType> calendarFilter, String tz) {

        List<CalendarEventDto> events = new ArrayList<>();

        // 1. Module adapters
        for (CalendarAdapter adapter : adapters) {
            if (calendarFilter == null || calendarFilter.contains(adapter.getCalendarType())) {
                try {
                    events.addAll(adapter.fetchEvents(communityId, userId, from, to));
                } catch (Exception e) {
                    log.warn("[Calendar] adapter {} failed: {}", adapter.getSource(), e.getMessage());
                }
            }
        }

        // 2. Personal / family events
        boolean wantPersonal = calendarFilter == null || calendarFilter.contains(CalendarType.MY_CALENDAR);
        boolean wantFamily   = calendarFilter == null || calendarFilter.contains(CalendarType.FAMILY_CALENDAR);

        if (wantPersonal) {
            List<PersonalCalendarEvent> personal = personalRepo
                    .findByCreatorUserIdAndStartTimeBetween(userId, from, to);
            personal.stream().filter(e -> e.getRrule() == null)
                    .map(e -> personalService.toDto(e, userId))
                    .forEach(events::add);
            personal.stream().filter(e -> e.getRrule() != null)
                    .forEach(e -> rruleExpander.expand(e, from, to)
                            .forEach(times -> events.add(toDtoWithTimes(e, times[0], times[1], userId))));
            personalRepo.findByCreatorUserIdAndRruleIsNotNullAndRruleUntilAfter(userId, from)
                    .stream().filter(e -> e.getStartTime().isBefore(from))
                    .forEach(e -> rruleExpander.expand(e, from, to)
                            .forEach(times -> events.add(toDtoWithTimes(e, times[0], times[1], userId))));
        }

        // 3. Family calendar — only FAMILY_CALENDAR events from family members
        if (wantFamily) {
            List<Long> memberIds = familyRepo.findByUserId(userId).stream()
                    .flatMap(m -> familyRepo.findByFamilyId(m.getFamilyId()).stream())
                    .map(FamilyCalendarMember::getUserId)
                    .filter(id -> !id.equals(userId))
                    .distinct().toList();
            for (Long memberId : memberIds) {
                personalRepo.findByCreatorUserIdAndStartTimeBetween(memberId, from, to)
                        .stream().filter(e -> CalendarType.FAMILY_CALENDAR.name().equals(e.getCalendarType()))
                        .map(e -> personalService.toDto(e, userId))
                        .forEach(events::add);
            }
        }

        events.sort(Comparator.comparing(CalendarEventDto::startTime));
        return events;
    }

    public String exportIcs(Long communityId, Long userId, Instant from, Instant to, List<CalendarType> filter) {
        List<CalendarEventDto> events = getEvents(communityId, userId, from, to, filter, "UTC");
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Mana Community//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        for (CalendarEventDto event : events) {
            if (event.isCancelled()) continue;
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(event.id().replace(":", "-")).append("@manacommunity.app\r\n");
            sb.append("DTSTART:").append(formatIcsDate(event.startTime())).append("\r\n");
            sb.append("DTEND:").append(formatIcsDate(event.endTime())).append("\r\n");
            sb.append("SUMMARY:").append(icsEscape(event.title())).append("\r\n");
            if (event.description() != null)
                sb.append("DESCRIPTION:").append(icsEscape(event.description())).append("\r\n");
            if (event.location() != null)
                sb.append("LOCATION:").append(icsEscape(event.location())).append("\r\n");
            sb.append("STATUS:CONFIRMED\r\n");
            sb.append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    public List<FamilyCalendarMember> getFamilyMembers(Long userId) {
        return familyRepo.findByUserId(userId).stream()
                .flatMap(m -> familyRepo.findByFamilyId(m.getFamilyId()).stream())
                .filter(m -> !m.getUserId().equals(userId))
                .distinct().toList();
    }

    private String formatIcsDate(Instant instant) {
        ZonedDateTime zdt = instant.atZone(ZoneOffset.UTC);
        return String.format("%04d%02d%02dT%02d%02d%02dZ",
                zdt.getYear(), zdt.getMonthValue(), zdt.getDayOfMonth(),
                zdt.getHour(), zdt.getMinute(), zdt.getSecond());
    }

    private String icsEscape(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    private CalendarEventDto toDtoWithTimes(PersonalCalendarEvent e, Instant start, Instant end, Long viewerUserId) {
        boolean hasReminder = reminderRepo.existsByUserIdAndEventSourceAndSourceId(
                viewerUserId, EventSource.PERSONAL.name(), String.valueOf(e.getId()));
        CalendarType ct = CalendarType.valueOf(e.getCalendarType());
        String color = e.getColor() != null ? e.getColor() : "#2563EB";
        return new CalendarEventDto(
                "PERSONAL:" + e.getId() + ":" + start.getEpochSecond(),
                e.getTitle(), e.getDescription(), e.getLocation(),
                start, end, e.isAllDay(), e.getTimezone(),
                ct, EventSource.PERSONAL,
                String.valueOf(e.getId()), color,
                e.isCancelled(), hasReminder, "PERSONAL_EVENT", String.valueOf(e.getId()), null
        );
    }
}
