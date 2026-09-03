package com.manacommunity.api.events.dto;

import java.util.List;

/**
 * Slim DTOs for the user-facing event dashboard.
 *
 * Design intent:
 *   - DashboardPayload  → single response for GET /api/events/user-dashboard
 *                         replaces the current 10-call fan-out on mount
 *   - EventCardItem     → 11-field projection; replaces the 30+ field EventResponse on list
 *   - MyActivitiesPayload → lazy-loaded per event on modal open
 *   - ScheduledActivitiesPayload → live scheduled activities (pooja, meal, cultural) per event
 */
public final class EventUserDashboardResponse {

    private EventUserDashboardResponse() {}

    // ── Top-level payload ────────────────────────────────────────────────────

    public record DashboardPayload(
            UserStats stats,
            List<EventCardItem> upcomingEvents,
            List<MyRegistrationItem> myRegistrations,
            List<PendingItem> pendingActions,
            UserPassSummaryView passSummary,
            List<FamilyMemberItem> familyMembers
    ) {
        public DashboardPayload(UserStats stats, List<EventCardItem> upcomingEvents, List<MyRegistrationItem> myRegistrations, List<PendingItem> pendingActions, UserPassSummaryView passSummary) {
            this(stats, upcomingEvents, myRegistrations, pendingActions, passSummary, java.util.Collections.emptyList());
        }

        public DashboardPayload(UserStats stats, List<EventCardItem> upcomingEvents, List<MyRegistrationItem> myRegistrations, List<PendingItem> pendingActions) {
            this(stats, upcomingEvents, myRegistrations, pendingActions, null, java.util.Collections.emptyList());
        }
    }

    public record FamilyMemberItem(
            Long id,
            String name,
            String relation,
            Integer age,
            String gender,
            String gothram,
            String phone,
            String dob
    ) {}

    // ── User stats (KPI bar) ─────────────────────────────────────────────────

    public record UserStats(
            long upcomingCount,
            long myRegistrationsCount,
            long myPoojaCount,
            long myMealCount,
            long myCulturalCount,
            long totalPassesCount,
            long myPoojaPassesCount,
            long myMealPassesCount,
            long myCulturalPassesCount
    ) {
        public UserStats(long upcomingCount, long myRegistrationsCount, long myPoojaCount, long myMealCount, long myCulturalCount) {
            this(upcomingCount, myRegistrationsCount, myPoojaCount, myMealCount, myCulturalCount, 0, 0, 0, 0);
        }
    }

    // ── Slim event card (list view) ──────────────────────────────────────────

    public record EventCardItem(
            Long id,
            String title,
            String type,
            String status,
            String startDate,
            String endDate,
            String startTime,
            String endTime,
            String location,
            String city,
            String imageUrl,
            String priceType,
            Double price,
            boolean registered,
            int attendeeCount,
            Integer maxAttendees,
            String registrationDeadline,
            ActivityFlags activitySummary
    ) {}

    // ── Activity presence flags (avoids 3 extra calls per card) ─────────────

    public record ActivityFlags(
            boolean hasPooja,
            boolean hasMeal,
            boolean hasCultural,
            long poojaCount,
            long mealCount,
            long culturalCount
    ) {
        public ActivityFlags(boolean hasPooja, boolean hasMeal, boolean hasCultural) {
            this(hasPooja, hasMeal, hasCultural, hasPooja ? 1 : 0, hasMeal ? 1 : 0, hasCultural ? 1 : 0);
        }
    }

    // ── User's own registrations (slim) ──────────────────────────────────────

    public record MyRegistrationItem(
            Long registrationId,
            Long eventId,
            String eventTitle,
            String category,
            String status,
            String registeredAt,
            String eventStartDate
    ) {}

    // ── Pending actions scoped to the logged-in user ─────────────────────────

    public record PendingItem(
            String id,
            String type,
            String message,
            Long eventId,
            String eventTitle,
            String priority
    ) {}

    // ── Lazy-loaded user activity detail (fetched only when modal opens) ─────

    public record MyActivitiesPayload(
            Long eventId,
            List<ActivityItem> pooja,
            List<ActivityItem> meals,
            List<ActivityItem> cultural
    ) {}

    public record ActivityItem(
            Long id,
            String activityTitle,
            String status,
            String date,
            String time,
            String registeredAt
    ) {}

    // ── Scheduled activities per event for dashboard/registration modal ──────

    public record ScheduledActivitiesPayload(
            Long eventId,
            long livePoojaCount,
            long liveMealCount,
            long liveCulturalCount,
            List<PoojaScheduledActivityView> poojaActivities,
            List<LunchDinnerDashboardView> meals,
            List<CulturalScheduledActivityView> culturalActivities
    ) {}
}

