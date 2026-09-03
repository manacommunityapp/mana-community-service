package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.CulturalScheduledActivityView;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.ActivityFlags;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.ActivityItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.DashboardPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.EventCardItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.MyActivitiesPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.MyRegistrationItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.PendingItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.ScheduledActivitiesPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.UserStats;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.FamilyMemberItem;
import com.manacommunity.api.events.dto.LunchDinnerDashboardView;
import com.manacommunity.api.events.dto.PoojaScheduledActivityView;
import com.manacommunity.api.events.dto.UserPassSummaryView;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.media.service.MediaUrlService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventUserDashboardService {

    private final EventCommunityRepository eventRepo;
    private final EventBookingRegistrationRepository bookingRegRepo;
    private final PoojaSevaRepository poojaSevaRepo;
    private final LunchDinnerRepository lunchDinnerRepo;
    private final CulturalEventRepository culturalEventRepo;
    private final MediaRepository mediaRepo;
    private final MediaUrlService mediaUrlService;
    private final FamilyMemberRepository familyMemberRepo;

    // ── Main dashboard payload ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardPayload getDashboard(AppUser user) {
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Long userId = user.getId();

        if (communityId == null) {
            UserStats empty = new UserStats(0, 0, 0, 0, 0, 0, 0, 0, 0);
            return new DashboardPayload(empty, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null);
        }

        // 1. Slim event cards — upcoming only, no sub-resources
        List<EventCommunity> upcoming = eventRepo.findUpcomingByCommunity(communityId);
        List<Long> eventIds = upcoming.stream().map(EventCommunity::getId).toList();

        java.util.Map<Long, Long> poojaCountMap = new java.util.HashMap<>();
        java.util.Map<Long, Long> mealCountMap = new java.util.HashMap<>();
        java.util.Map<Long, Long> culturalCountMap = new java.util.HashMap<>();

        if (!eventIds.isEmpty()) {
            poojaSevaRepo.countActiveGroupedByMainEventIdIn(eventIds)
                    .forEach(v -> poojaCountMap.put(v.getEventId(), v.getCount()));
            lunchDinnerRepo.countActiveMealsGroupedByMainEventIdIn(eventIds)
                    .forEach(v -> mealCountMap.put(v.getEventId(), v.getCount()));
            culturalEventRepo.countActiveGroupedByMainEventIdIn(eventIds)
                    .forEach(v -> culturalCountMap.put(v.getEventId(), v.getCount()));
        }

        List<EventCardItem> cards = upcoming.stream()
                .map(e -> toCardItem(e, userId,
                        poojaCountMap.getOrDefault(e.getId(), 0L),
                        mealCountMap.getOrDefault(e.getId(), 0L),
                        culturalCountMap.getOrDefault(e.getId(), 0L)))
                .toList();

        // 2. My registrations — single native JOIN query (fixes N+1 from eventRepo.findById per row)
        //    Native result: [0]=id [1]=main_event_id [2]=activity_title [3]=category
        //                   [4]=status [5]=created_at (Timestamp) [6]=start_date (Date/String)
        List<Object[]> regRows = bookingRegRepo.findUserRegProjectionsWithEventDate(userId);

        List<MyRegistrationItem> myRegs = regRows.stream()
                .map(row -> {
                    String registeredAt = null;
                    if (row[5] != null) {
                        // Native query returns java.sql.Timestamp — format to ISO string
                        registeredAt = row[5] instanceof java.sql.Timestamp
                                ? ((java.sql.Timestamp) row[5]).toLocalDateTime()
                                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : row[5].toString();
                    }
                    String eventStartDate = row[6] != null ? row[6].toString() : null;
                    return new MyRegistrationItem(
                            ((Number) row[0]).longValue(),
                            row[1] != null ? ((Number) row[1]).longValue() : null,
                            (String) row[2],
                            (String) row[3],
                            (String) row[4],
                            registeredAt,
                            eventStartDate
                    );
                })
                .toList();

        // 3. Pending payment actions — separate fetch to keep the JOIN query slim
        List<EventBookingRegistration> pendingRegs = bookingRegRepo
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(b -> "PENDING".equalsIgnoreCase(b.getPaymentStatus())
                        && !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .toList();

        List<PendingItem> pending = pendingRegs.stream()
                .map(b -> new PendingItem(
                        "pay-" + b.getId(),
                        "PAYMENT_PENDING",
                        "Payment pending for: " + (b.getActivityTitle() != null ? b.getActivityTitle() : "Event"),
                        b.getMainEventId(),
                        b.getActivityTitle(),
                        "high"
                ))
                .toList();

        // 4. User stats & pass counts breakdown using active event IDs directly across 4 booking tables
        UserPassSummaryView passSummary = !eventIds.isEmpty()
                ? bookingRegRepo.countPassSummaryByUserAndActiveEvents(userId, eventIds)
                : null;
        long myRegistrationsCount = bookingRegRepo.countUserRegistrations(userId);
        long myPoojaCount        = bookingRegRepo.countUserPoojaRegistrations(userId);
        long myMealCount         = bookingRegRepo.countUserMealRegistrations(userId);
        long myCulturalCount     = bookingRegRepo.countUserCulturalRegistrations(userId);

        long totalPasses    = passSummary != null && passSummary.getTotalPasses() != null ? passSummary.getTotalPasses() : 0;
        long poojaPasses    = passSummary != null && passSummary.getPoojaPasses() != null ? passSummary.getPoojaPasses() : 0;
        long mealPasses     = passSummary != null && passSummary.getMealPasses() != null ? passSummary.getMealPasses() : 0;
        long culturalPasses = passSummary != null && passSummary.getCulturalPasses() != null ? passSummary.getCulturalPasses() : 0;

        UserStats stats = new UserStats(
                cards.size(),
                myRegistrationsCount,
                myPoojaCount,
                myMealCount,
                myCulturalCount,
                totalPasses,
                poojaPasses,
                mealPasses,
                culturalPasses
        );

        // 5. User's family members (slim DTO - no heavy user/community entity graph)
        List<FamilyMemberItem> familyMembers = familyMemberRepo.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(m -> new FamilyMemberItem(
                        m.getId(),
                        m.getName(),
                        m.getRelation(),
                        m.getAge(),
                        m.getGender(),
                        m.getGothram(),
                        m.getPhone(),
                        m.getDob()
                ))
                .toList();

        return new DashboardPayload(stats, cards, myRegs, pending, passSummary, familyMembers);
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberItem> getFamilyMembers(AppUser user) {
        if (user == null || user.getId() == null) return Collections.emptyList();
        return familyMemberRepo.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(m -> new FamilyMemberItem(
                        m.getId(),
                        m.getName(),
                        m.getRelation(),
                        m.getAge(),
                        m.getGender(),
                        m.getGothram(),
                        m.getPhone(),
                        m.getDob()
                ))
                .toList();
    }

    // ── Shared filter constants ───────────────────────────────────────────────

    /** Filter: registration-required activities only (needsRegistration = true). */
    private static final List<Boolean> REG_REQUIRED  = List.of(true);

    /** Filter: open-to-all activities only (needsRegistration = false). */
    private static final List<Boolean> OPEN_TO_ALL   = List.of(false);

    /** Filter: all activities regardless of registration requirement. */
    private static final List<Boolean> ALL_ACTIVITIES = List.of(true, false);

    // ── Scheduled activities per event (Pooja, Meals, Cultural) ──────────────

    /**
     * Returns scheduled activities that REQUIRE registration (needsRegistration = true).
     * Used by GET /api/events/user-dashboard/{eventId}/scheduled-activities
     */
    @Transactional(readOnly = true)
    public ScheduledActivitiesPayload getScheduledActivities(Long eventId) {
        List<PoojaScheduledActivityView> poojas    = poojaSevaRepo.findScheduledActivitiesForDashboard(eventId, REG_REQUIRED);
        List<LunchDinnerDashboardView>   meals     = lunchDinnerRepo.findMealsForDashboard(eventId, REG_REQUIRED);
        List<CulturalScheduledActivityView> culturals = culturalEventRepo.findCulturalEventsForDashboard(eventId, REG_REQUIRED);
        return new ScheduledActivitiesPayload(eventId, poojas.size(), meals.size(), culturals.size(), poojas, meals, culturals);
    }

    /** Registration-required pooja/seva activities only. */
    @Transactional(readOnly = true)
    public List<PoojaScheduledActivityView> getPoojaActivities(Long eventId) {
        return poojaSevaRepo.findScheduledActivitiesForDashboard(eventId, REG_REQUIRED);
    }

    /** Registration-required meal activities only. */
    @Transactional(readOnly = true)
    public List<LunchDinnerDashboardView> getMealActivities(Long eventId) {
        return lunchDinnerRepo.findMealsForDashboard(eventId, REG_REQUIRED);
    }

    /** Registration-required cultural activities only. */
    @Transactional(readOnly = true)
    public List<CulturalScheduledActivityView> getCulturalActivities(Long eventId) {
        return culturalEventRepo.findCulturalEventsForDashboard(eventId, REG_REQUIRED);
    }

    // ── Open-to-all variants (needsRegistration = false only) ─────────────────

    /**
     * Returns only open-to-all activities (needsRegistration = false).
     * Used by GET /api/events/user-dashboard/{eventId}/open-scheduled-activities
     */
    @Transactional(readOnly = true)
    public ScheduledActivitiesPayload getOpenScheduledActivities(Long eventId) {
        List<PoojaScheduledActivityView>    poojas    = poojaSevaRepo.findScheduledActivitiesForDashboard(eventId, OPEN_TO_ALL);
        List<LunchDinnerDashboardView>      meals     = lunchDinnerRepo.findMealsForDashboard(eventId, OPEN_TO_ALL);
        List<CulturalScheduledActivityView> culturals = culturalEventRepo.findCulturalEventsForDashboard(eventId, OPEN_TO_ALL);
        return new ScheduledActivitiesPayload(eventId, poojas.size(), meals.size(), culturals.size(), poojas, meals, culturals);
    }

    // ── All-activities variants (needsRegistration = true AND false) ──────────

    /**
     * Returns ALL active scheduled activities (pooja, meals, cultural) for an event
     * including those where needsRegistration = false (open-to-all).
     * Used by GET /api/events/user-dashboard/{eventId}/all-scheduled-activities
     */
    @Transactional(readOnly = true)
    public ScheduledActivitiesPayload getAllScheduledActivities(Long eventId) {
        List<PoojaScheduledActivityView>    poojas    = poojaSevaRepo.findScheduledActivitiesForDashboard(eventId, ALL_ACTIVITIES);
        List<LunchDinnerDashboardView>      meals     = lunchDinnerRepo.findMealsForDashboard(eventId, ALL_ACTIVITIES);
        List<CulturalScheduledActivityView> culturals = culturalEventRepo.findCulturalEventsForDashboard(eventId, ALL_ACTIVITIES);
        return new ScheduledActivitiesPayload(eventId, poojas.size(), meals.size(), culturals.size(), poojas, meals, culturals);
    }

    /** All active pooja/seva activities — including open-to-all sevas. */
    @Transactional(readOnly = true)
    public List<PoojaScheduledActivityView> getAllPoojaActivities(Long eventId) {
        return poojaSevaRepo.findScheduledActivitiesForDashboard(eventId, ALL_ACTIVITIES);
    }

    /** All active upcoming meals — including open/free meals. */
    @Transactional(readOnly = true)
    public List<LunchDinnerDashboardView> getAllMealActivities(Long eventId) {
        return lunchDinnerRepo.findMealsForDashboard(eventId, ALL_ACTIVITIES);
    }

    /** All active upcoming cultural activities — including open performances. */
    @Transactional(readOnly = true)
    public List<CulturalScheduledActivityView> getAllCulturalActivities(Long eventId) {
        return culturalEventRepo.findCulturalEventsForDashboard(eventId, ALL_ACTIVITIES);
    }

    @Transactional(readOnly = true)
    public UserPassSummaryView getUserPassSummary(Long userId, Long communityId, Long eventId) {
        if (eventId != null) {
            return bookingRegRepo.countActiveUserPassSummaryByCommunityAndEvent(userId, communityId, eventId);
        }
        return bookingRegRepo.countActiveUserPassSummaryByCommunity(userId, communityId);
    }

    // ── Per-event activity detail (lazy, modal-only) ─────────────────────────

    @Transactional(readOnly = true)
    public MyActivitiesPayload getMyActivities(Long eventId, Long userId) {
        List<ActivityItem> poojaItems = new ArrayList<>();
        List<ActivityItem> mealItems = new ArrayList<>();
        List<ActivityItem> culturalItems = new ArrayList<>();

        bookingRegRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(b -> eventId.equals(b.getMainEventId()) && !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .forEach(b -> {
                    String cat = b.getCategory() != null ? b.getCategory().toLowerCase() : "";
                    ActivityItem item = new ActivityItem(
                            b.getId(),
                            b.getActivityTitle() != null ? b.getActivityTitle() : b.getCategory(),
                            b.getStatus(),
                            b.getEventDate(),
                            b.getEventTime(),
                            formatDt(b)
                    );
                    if (cat.contains("pooja") || cat.contains("seva")) {
                        poojaItems.add(item);
                    } else if (cat.contains("meal") || cat.contains("food") || cat.contains("lunch") || cat.contains("dinner")) {
                        mealItems.add(item);
                    } else if (cat.contains("cultural") || cat.contains("cult")) {
                        culturalItems.add(item);
                    }
                });

        return new MyActivitiesPayload(eventId, poojaItems, mealItems, culturalItems);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private EventCardItem toCardItem(EventCommunity e, Long userId, long poojaCount, long mealCount, long culturalCount) {
        // Resolve CDN URL — never returns base64
        String imageUrl = null;
        if (e.getImageMediaExternalId() != null) {
            imageUrl = mediaRepo.findByExternalIdAndDeletedFalse(e.getImageMediaExternalId())
                    .map(mediaUrlService::generateUrl)
                    .orElse(e.getImageUrl());
        } else {
            imageUrl = e.getImageUrl();
        }

        // isRegistered — two cheap exists() queries instead of fetching full objects
        boolean isRegistered = userId != null && (
                bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(userId, "event-" + e.getId(), "CANCELLED")
                || bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(userId, String.valueOf(e.getId()), "CANCELLED")
        );

        // Attendee count via a single count query
        long attendeeCount = bookingRegRepo.countByMainEventIdAndStatusNot(e.getId(), "CANCELLED");

        return new EventCardItem(
                e.getId(),
                e.getTitle(),
                e.getType() != null ? e.getType().name() : "GENERAL",
                e.getStatus() != null ? e.getStatus().name() : "PUBLISHED",
                e.getStartDate() != null ? e.getStartDate().toString() : null,
                e.getEndDate() != null ? e.getEndDate().toString() : null,
                e.getStartTime() != null ? e.getStartTime().toString() : null,
                e.getEndTime() != null ? e.getEndTime().toString() : null,
                e.getLocation(),
                e.getCity(),
                imageUrl,
                e.getPriceType() != null ? e.getPriceType().name() : "FREE",
                e.getPrice(),
                isRegistered,
                (int) attendeeCount,
                e.getMaxAttendees() != null ? e.getMaxAttendees() : e.getCapacity(),
                e.getRegistrationDeadline() != null ? e.getRegistrationDeadline().toString() : null,
                new ActivityFlags(poojaCount > 0, mealCount > 0, culturalCount > 0, poojaCount, mealCount, culturalCount)
        );
    }

    private String formatDt(EventBookingRegistration b) {
        if (b.getCreatedAt() != null) {
            return b.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return null;
    }
}


