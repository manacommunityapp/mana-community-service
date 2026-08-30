package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventUserDashboardResponse.ActivityFlags;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.ActivityItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.DashboardPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.EventCardItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.MyActivitiesPayload;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.MyRegistrationItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.PendingItem;
import com.manacommunity.api.events.dto.EventUserDashboardResponse.UserStats;
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

    // ── Main dashboard payload ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardPayload getDashboard(AppUser user) {
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Long userId = user.getId();

        if (communityId == null) {
            UserStats empty = new UserStats(0, 0, 0, 0, 0);
            return new DashboardPayload(empty, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }

        // 1. Slim event cards — upcoming only, no sub-resources
        List<EventCommunity> upcoming = eventRepo.findUpcomingByCommunity(communityId);
        List<EventCardItem> cards = upcoming.stream()
                .map(e -> toCardItem(e, userId))
                .toList();

        // 2. User's registrations — single query, filtered in-stream
        List<EventBookingRegistration> allUserRegs = bookingRegRepo.findByUserIdOrderByCreatedAtDesc(userId);

        List<MyRegistrationItem> myRegs = allUserRegs.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getMainEventId() != null)
                .map(b -> {
                    String eventStartDate = eventRepo.findById(b.getMainEventId())
                            .map(e -> e.getStartDate() != null ? e.getStartDate().toString() : null)
                            .orElse(null);
                    return new MyRegistrationItem(
                            b.getId(),
                            b.getMainEventId(),
                            b.getActivityTitle(),
                            b.getCategory(),
                            b.getStatus(),
                            formatDt(b),
                            eventStartDate
                    );
                })
                .toList();

        // 3. Pending payment actions — filtered from the same user regs list
        List<PendingItem> pending = allUserRegs.stream()
                .filter(b -> "PENDING".equalsIgnoreCase(b.getPaymentStatus())
                        && !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .map(b -> new PendingItem(
                        "pay-" + b.getId(),
                        "PAYMENT_PENDING",
                        "Payment pending for: " + (b.getActivityTitle() != null ? b.getActivityTitle() : "Event"),
                        b.getMainEventId(),
                        b.getActivityTitle(),
                        "high"
                ))
                .toList();

        // 4. User stats — derived from data already in memory, zero extra queries
        long myPoojaCount = allUserRegs.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getCategory() != null && (
                        b.getCategory().toLowerCase().contains("pooja")
                        || b.getCategory().toLowerCase().contains("seva")))
                .count();

        long myMealCount = allUserRegs.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getCategory() != null && (
                        b.getCategory().toLowerCase().contains("meal")
                        || b.getCategory().toLowerCase().contains("food")
                        || b.getCategory().toLowerCase().contains("lunch")
                        || b.getCategory().toLowerCase().contains("dinner")))
                .count();

        long myCulturalCount = allUserRegs.stream()
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                .filter(b -> b.getCategory() != null && (
                        b.getCategory().toLowerCase().contains("cultural")
                        || b.getCategory().toLowerCase().contains("cult")))
                .count();

        UserStats stats = new UserStats(
                cards.size(),
                myRegs.size(),
                myPoojaCount,
                myMealCount,
                myCulturalCount
        );

        return new DashboardPayload(stats, cards, myRegs, pending);
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

    private EventCardItem toCardItem(EventCommunity e, Long userId) {
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

        // Activity presence flags — existence only, no full list fetch
        boolean hasPooja = !poojaSevaRepo.findByMainEventIdOrderByDateAscStartTimeAsc(e.getId()).isEmpty();
        boolean hasMeal = !lunchDinnerRepo.findByMainEventIdOrderByDateAscStartTimeAsc(e.getId()).isEmpty();
        boolean hasCultural = !culturalEventRepo.findByMainEventIdOrderByDateAscStartTimeAscSortOrderAsc(e.getId()).isEmpty();

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
                new ActivityFlags(hasPooja, hasMeal, hasCultural)
        );
    }

    private String formatDt(EventBookingRegistration b) {
        if (b.getCreatedAt() != null) {
            return b.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return null;
    }
}
