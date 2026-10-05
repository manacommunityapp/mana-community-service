package com.manacommunity.api.calendar.controller;

import com.manacommunity.api.calendar.dto.CalendarDtos.*;
import com.manacommunity.api.calendar.model.CalendarDomain;
import com.manacommunity.api.calendar.service.ManaCalendarService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class ManaCalendarController {

    private final ManaCalendarService calendarService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/timeline")
    public ResponseEntity<List<CalendarEventItemDto>> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) CalendarDomain domain,
            @RequestParam(required = false, defaultValue = "false") boolean onlyMine,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(calendarService.getTimeline(user, from, to, domain, onlyMine, q));
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventItemDto> createEvent(
            @Valid @RequestBody CreateCalendarEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(calendarService.createEvent(user, request));
    }

    @GetMapping("/month-summary")
    public ResponseEntity<CalendarMonthSummaryDto> getMonthSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return ResponseEntity.ok(calendarService.getMonthSummary(month));
    }
}
