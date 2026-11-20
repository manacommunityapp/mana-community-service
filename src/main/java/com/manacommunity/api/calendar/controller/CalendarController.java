package com.manacommunity.api.calendar.controller;

import com.manacommunity.api.calendar.CalendarType;
import com.manacommunity.api.calendar.dto.CalendarEventDto;
import com.manacommunity.api.calendar.dto.PersonalEventRequest;
import com.manacommunity.api.calendar.dto.ReminderRequest;
import com.manacommunity.api.calendar.entity.FamilyCalendarMember;
import com.manacommunity.api.calendar.service.CalendarAggregatorService;
import com.manacommunity.api.calendar.service.CalendarReminderService;
import com.manacommunity.api.calendar.service.PersonalCalendarEventService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarAggregatorService aggregator;
    private final PersonalCalendarEventService personalService;
    private final CalendarReminderService reminderService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> getEvents(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) List<CalendarType> calendars,
            @RequestParam(defaultValue = "UTC") String tz,
            @AuthenticationPrincipal UserPrincipal principal) {
        Instant fromI = LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toI   = LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return ResponseEntity.ok(aggregator.getEvents(
                principal.getCommunityId(), principal.getUserId(), fromI, toI, calendars, tz));
    }

    @PostMapping("/personal")
    public ResponseEntity<CalendarEventDto> createPersonal(
            @RequestBody @Valid PersonalEventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201)
                .body(personalService.create(req, principal.getCommunityId(), principal.getUserId()));
    }

    @PutMapping("/personal/{id}")
    public ResponseEntity<CalendarEventDto> updatePersonal(
            @PathVariable Long id,
            @RequestBody @Valid PersonalEventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(personalService.update(id, req, principal.getUserId()));
    }

    @DeleteMapping("/personal/{id}")
    public ResponseEntity<Void> deletePersonal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        personalService.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reminders")
    public ResponseEntity<Void> setReminder(
            @RequestBody @Valid ReminderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        reminderService.setReminder(principal.getUserId(), req.eventSource(), req.sourceId(),
                req.minutesBefore(), Instant.parse(req.eventStartTime()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reminders")
    public ResponseEntity<Void> removeReminder(
            @RequestParam String eventSource,
            @RequestParam String sourceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        reminderService.removeReminder(principal.getUserId(), eventSource, sourceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/ics", produces = "text/calendar")
    public ResponseEntity<String> exportIcs(
            @RequestParam(defaultValue = "2026-01-01") String from,
            @RequestParam(defaultValue = "2027-12-31") String to,
            @RequestParam(required = false) List<CalendarType> calendars,
            @AuthenticationPrincipal UserPrincipal principal) {
        Instant fromI = LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toI   = LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        String ics = aggregator.exportIcs(
                principal.getCommunityId(), principal.getUserId(), fromI, toI, calendars);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"mana-calendar.ics\"")
                .body(ics);
    }

    @GetMapping("/family")
    public ResponseEntity<List<FamilyCalendarMember>> getFamily(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(aggregator.getFamilyMembers(principal.getUserId()));
    }
}
