package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.NotificationStatsResponse;
import com.manacommunity.api.events.dto.ScheduleNotificationRequest;
import com.manacommunity.api.events.dto.ScheduledNotificationResponse;
import com.manacommunity.api.events.service.EventScheduledNotificationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/notifications")
@RequiredArgsConstructor
public class EventNotificationController {

    private final EventScheduledNotificationService notificationService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<ScheduledNotificationResponse> schedule(
            @PathVariable Long eventId,
            @RequestBody ScheduleNotificationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        req.setEventId(eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.schedule(eventId, req, user));
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<ScheduledNotificationResponse> sendNow(
            @PathVariable Long eventId,
            @RequestBody ScheduleNotificationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        req.setEventId(eventId);
        req.setSendNow(true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.schedule(eventId, req, user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<Page<ScheduledNotificationResponse>> list(
            @PathVariable Long eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.list(eventId, status, page, size));
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<ScheduledNotificationResponse> getById(
            @PathVariable Long eventId,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.getById(eventId, notificationId));
    }

    @PutMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<ScheduledNotificationResponse> update(
            @PathVariable Long eventId,
            @PathVariable Long notificationId,
            @RequestBody ScheduleNotificationRequest req) {
        return ResponseEntity.ok(notificationService.update(eventId, notificationId, req));
    }

    @PutMapping("/{notificationId}/pause")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<ScheduledNotificationResponse> pause(
            @PathVariable Long eventId,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.pause(eventId, notificationId));
    }

    @PutMapping("/{notificationId}/resume")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<ScheduledNotificationResponse> resume(
            @PathVariable Long eventId,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.resume(eventId, notificationId));
    }

    @PostMapping("/{notificationId}/resend")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<ScheduledNotificationResponse> resend(
            @PathVariable Long eventId,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.resend(eventId, notificationId));
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<Void> cancel(
            @PathVariable Long eventId,
            @PathVariable Long notificationId) {
        notificationService.cancel(eventId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<NotificationStatsResponse> getStats(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(notificationService.getStats(eventId));
    }
}
