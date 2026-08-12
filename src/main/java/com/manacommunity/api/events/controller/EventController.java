package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.DashboardAnalyticsResponse;
import com.manacommunity.api.events.dto.DashboardStatsResponse;
import com.manacommunity.api.events.dto.EventRequest;
import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.dto.PendingActionItemDto;
import com.manacommunity.api.events.dto.RegistrationResponse;
import com.manacommunity.api.events.service.EventService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(eventService.getUpcomingEvents(communityId, type, user.getId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(eventService.getAllEvents(communityId, user.getId()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventResponse>> getMyEvents(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getMyEvents(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<EventResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getById(id, user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody EventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.update(id, req, user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        eventService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(DashboardStatsResponse.builder().build());
        return ResponseEntity.ok(eventService.getDashboardStats(communityId));
    }

    @GetMapping("/dashboard/analytics")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<DashboardAnalyticsResponse> getDashboardAnalytics(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(DashboardAnalyticsResponse.builder().build());
        return ResponseEntity.ok(eventService.getDashboardAnalytics(communityId));
    }

    @GetMapping("/dashboard/pending-actions")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<PendingActionItemDto>> getPendingActionItems(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.getPendingActionItems(communityId));
    }

    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<RegistrationResponse>> getEventRegistrations(
            @PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventRegistrations(id));
    }

    @PutMapping("/registrations/{regId}/confirm")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<RegistrationResponse> confirmRegistration(
            @PathVariable Long regId) {
        return ResponseEntity.ok(eventService.confirmRegistration(regId));
    }

    @PutMapping("/registrations/{regId}/reject")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<RegistrationResponse> rejectRegistration(
            @PathVariable Long regId) {
        return ResponseEntity.ok(eventService.rejectRegistration(regId));
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasAuthority('Register Event')")
    public ResponseEntity<EventResponse> register(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.register(id, user));
    }

    @DeleteMapping("/{id}/register")
    @PreAuthorize("hasAuthority('Register Event')")
    public ResponseEntity<EventResponse> unregister(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.unregister(id, user.getId()));
    }
}
