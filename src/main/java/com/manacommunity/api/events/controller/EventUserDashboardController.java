package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.CulturalScheduledActivityView;
import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.DashboardPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.MyActivitiesPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.ScheduledActivitiesPayload;
import com.manacommunity.api.events.dto.LunchDinnerDashboardView;
import com.manacommunity.api.events.dto.PoojaScheduledActivityView;
import com.manacommunity.api.events.dto.UserPassSummaryView;
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

import java.util.List;

/**
 * Optimised read-only endpoints for the user-facing event dashboard.
 *
 * Replaces multi-call fan-outs with targeted, projection-based endpoints:
 *   GET /api/events/user-dashboard                          → consolidated payload & pass counts (mount call)
 *   GET /api/events/user-dashboard/{id}/detail              → full event data (modal open)
 *   GET /api/events/user-dashboard/{id}/my-activities       → user's activity registrations (modal open)
 *   GET /api/events/user-dashboard/{id}/scheduled-activities → live scheduled activities (pooja, meals, cultural)
 *   GET /api/events/user-dashboard/passes-summary           → user pass & devotee breakdown
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
     *  - stats         KPI bar (counters and pass counts breakdown)
     *  - upcomingEvents  Slim EventCardItem list (with live activity count flags)
     *  - myRegistrations User's own registrations across all events
     *  - pendingActions  Payment-pending items scoped to this user
     *  - passSummary     Detailed pass/devotee count breakdown
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
     */
    @GetMapping("/{eventId}/my-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event')")
    public ResponseEntity<MyActivitiesPayload> getMyActivities(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getMyActivities(eventId, user.getId()));
    }

    /**
     * Live scheduled activities for an event (Pooja/Seva, Meals, Cultural).
     *
     * GET /api/events/user-dashboard/{eventId}/scheduled-activities
     */
    @GetMapping("/{eventId}/scheduled-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<ScheduledActivitiesPayload> getScheduledActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getScheduledActivities(eventId));
    }

    /**
     * Live scheduled Pooja / Seva activities for an event.
     *
     * GET /api/events/user-dashboard/{eventId}/pooja-activities
     */
    @GetMapping("/{eventId}/pooja-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<List<PoojaScheduledActivityView>> getPoojaActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getPoojaActivities(eventId));
    }

    /**
     * Live scheduled Lunch / Dinner meal activities for an event.
     *
     * GET /api/events/user-dashboard/{eventId}/meal-activities
     */
    @GetMapping("/{eventId}/meal-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<List<LunchDinnerDashboardView>> getMealActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getMealActivities(eventId));
    }

    /**
     * Live scheduled Cultural activities for an event.
     *
     * GET /api/events/user-dashboard/{eventId}/cultural-activities
     */
    @GetMapping("/{eventId}/cultural-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<List<CulturalScheduledActivityView>> getCulturalActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getCulturalActivities(eventId));
    }

    // ── All-activities endpoints (needsRegistration = true AND false) ─────────

    /**
     * ALL active scheduled activities (pooja, meals, cultural) for an event,
     * including those where needsRegistration = false (open-to-all).
     *
     * GET /api/events/user-dashboard/{eventId}/all-scheduled-activities
     */
    @GetMapping("/{eventId}/all-scheduled-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<ScheduledActivitiesPayload> getAllScheduledActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getAllScheduledActivities(eventId));
    }

    /**
     * ALL active pooja/seva activities for an event (including open-to-all sevas).
     *
     * GET /api/events/user-dashboard/{eventId}/all-pooja-activities
     */
    @GetMapping("/{eventId}/all-pooja-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<List<PoojaScheduledActivityView>> getAllPoojaActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getAllPoojaActivities(eventId));
    }

    /**
     * ALL active upcoming meals for an event (including open/free meals).
     *
     * GET /api/events/user-dashboard/{eventId}/all-meal-activities
     */
    @GetMapping("/{eventId}/all-meal-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<List<LunchDinnerDashboardView>> getAllMealActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getAllMealActivities(eventId));
    }

    /**
     * ALL active upcoming cultural activities for an event (including open performances).
     *
     * GET /api/events/user-dashboard/{eventId}/all-cultural-activities
     */
    @GetMapping("/{eventId}/all-cultural-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<List<CulturalScheduledActivityView>> getAllCulturalActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getAllCulturalActivities(eventId));
    }

    // ── Open-to-all endpoints (needsRegistration = false only) ───────────────

    /**
     * Open-to-all scheduled activities (pooja, meals, cultural) where registration is NOT required.
     *
     * GET /api/events/user-dashboard/{eventId}/open-scheduled-activities
     */
    @GetMapping("/{eventId}/open-scheduled-activities")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<ScheduledActivitiesPayload> getOpenScheduledActivities(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getOpenScheduledActivities(eventId));
    }

    /**
     * User's pass & devotee breakdown across all events in their community.
     *
     * GET /api/events/user-dashboard/passes-summary
     */
    @GetMapping("/passes-summary")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<UserPassSummaryView> getPassesSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dashboardService.getUserPassSummary(user.getId(), communityId, null));
    }

    /**
     * User's pass & devotee breakdown for a specific event.
     *
     * GET /api/events/user-dashboard/{eventId}/passes-summary
     */
    @GetMapping("/{eventId}/passes-summary")
    @PreAuthorize("hasAnyAuthority('View Events', 'View Event Dashboard', 'Register Event')")
    public ResponseEntity<UserPassSummaryView> getEventPassesSummary(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(dashboardService.getUserPassSummary(user.getId(), communityId, eventId));
    }

    /**
     * Fast slim family members list for devotee selection.
     *
     * GET /api/events/user-dashboard/family-members
     */
    @GetMapping("/family-members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<com.manacommunity.api.events.dto.EventUserDashboardResponse.FamilyMemberItem>> getFamilyMembers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(dashboardService.getFamilyMembers(user));
    }
}


