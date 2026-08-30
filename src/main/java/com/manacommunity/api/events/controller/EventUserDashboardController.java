package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.DashboardPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.MyActivitiesPayload;
import com.manacommunity.api.events.service.EventService;
import com.manacommunity.api.events.service.EventUserDashboardService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Optimised read-only endpoints for the user-facing event dashboard.
 *
 * Replaces the current 10-call fan-out in EventsDashboard.tsx with 3 targeted endpoints:
 *
 *   GET /api/events/user-dashboard              → slim consolidated payload (mount call)
 *   GET /api/events/user-dashboard/{id}/detail  → full event data (modal open)
 *   GET /api/events/user-dashboard/{id}/my-activities → user's activity registrations (modal open)
 */
@RestController
@RequestMapping("/api/events/user-dashboard")
@RequiredArgsConstructor
public class EventUserDashboardController {

    private final EventUserDashboardService dashboardService;
    private final EventService eventService;
    private final LoggedInUserService loggedInUserService;

    /**
     * Single consolidated mount call for the user dashboard.
     *
     * Returns:
     *  - stats         KPI bar (5 counters, no extra queries — derived from data already fetched)
     *  - upcomingEvents  Slim EventCardItem list (11 fields each, CDN URLs, isRegistered flag)
     *  - myRegistrations User's own registrations across all events
     *  - pendingActions  Payment-pending items scoped to this user
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<DashboardPayload> getDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getDashboard(user));
    }

    /**
     * Full event detail — called only when the user opens an event's detail modal.
     *
     * Delegates to the existing EventService.getById so no duplication of mapping logic.
     * Returns the complete EventResponse including ticket types, contacts, payment info,
     * scanner URL, and notes — fields that are too heavy to include in the list payload.
     */
    @GetMapping("/{eventId}/detail")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<EventResponse> getEventDetail(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(eventService.getById(eventId, user.getId()));
    }

    /**
     * Lazy-loads this user's activity registrations (pooja, meal, cultural)
     * for a specific event.
     *
     * Called only when the detail modal is opened, not on dashboard mount.
     * Replaces the current eager calls to /pooja-registrations/my,
     * /cultural/registrations/my, and /{eventId}/meals.
     */
    @GetMapping("/{eventId}/my-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event')")
    public ResponseEntity<MyActivitiesPayload> getMyActivities(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getMyActivities(eventId, user.getId()));
    }
}
