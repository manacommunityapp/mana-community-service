package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyEventDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyEventService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kitty-events")
@RequiredArgsConstructor
public class KittyPartyEventController {

    private final KittyPartyEventService eventService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/circles/{circleId}")
    public ResponseEntity<EventResponse> createEvent(
            @PathVariable Long circleId,
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        EventResponse response = eventService.createEvent(user, circleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/circles/{circleId}")
    public ResponseEntity<List<EventResponse>> getEvents(
            @PathVariable Long circleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(eventService.getEventsByCircle(circleId, userId));
    }

    @GetMapping("/circles/{circleId}/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @PathVariable Long circleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(eventService.getUpcomingEvents(circleId, userId));
    }

    @GetMapping("/circles/{circleId}/summary")
    public ResponseEntity<EventSummaryDto> getEventSummary(
            @PathVariable Long circleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(eventService.getEventSummary(circleId, userId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(eventService.getEventById(eventId, userId));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = principal != null ? loggedInUserService.resolve(principal) : null;
        return ResponseEntity.ok(eventService.updateEvent(user, eventId, request));
    }

    @PostMapping("/{eventId}/rsvp")
    public ResponseEntity<RsvpResponse> submitRsvp(
            @PathVariable Long eventId,
            @Valid @RequestBody RsvpRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.submitRsvp(user, eventId, request));
    }

    @GetMapping("/{eventId}/rsvps")
    public ResponseEntity<List<RsvpResponse>> getEventRsvps(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventRsvps(eventId));
    }

    @PostMapping("/{eventId}/attendance")
    public ResponseEntity<AttendanceResponse> markAttendance(
            @PathVariable Long eventId,
            @Valid @RequestBody MarkAttendanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser marker = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.markAttendance(marker, eventId, request));
    }

    @GetMapping("/{eventId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getEventAttendance(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventAttendance(eventId));
    }
}
