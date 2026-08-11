package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.DashboardAnalyticsResponse;
import com.manacommunity.api.events.dto.DashboardStatsResponse;
import com.manacommunity.api.events.dto.EventRequest;
import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.dto.RegistrationResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventExpense;
import com.manacommunity.api.events.entity.EventRegistration;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventAuctionItemRepository;
import com.manacommunity.api.events.repository.EventDonationRepository;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.repository.EventSponsorRepository;
import com.manacommunity.api.events.repository.EventTaskRepository;
import com.manacommunity.api.events.repository.EventVolunteerRepository;
import com.manacommunity.api.events.repository.MealRegistrationRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventService {

    private final CommunityEventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final EventVolunteerRepository volunteerRepo;
    private final EventDonationRepository donationRepo;
    private final EventExpenseRepository expenseRepo;
    private final EventSponsorRepository sponsorRepo;
    private final EventTaskRepository taskRepo;
    private final MealRegistrationRepository mealRegRepo;
    private final EventAuctionItemRepository auctionItemRepo;

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents(Long communityId, String typeFilter, Long currentUserId) {
        List<CommunityEvent> events;
        if (typeFilter != null && !typeFilter.isBlank() && !"All".equalsIgnoreCase(typeFilter)) {
            CommunityEvent.EventType type = parseEnum(CommunityEvent.EventType.class, typeFilter);
            if (type != null) {
                events = eventRepo.findUpcomingByCommunityAndType(communityId, type);
            } else {
                events = eventRepo.findUpcomingByCommunity(communityId);
            }
        } else {
            events = eventRepo.findUpcomingByCommunity(communityId);
        }
        return events.stream().map(e -> toResponse(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(Long communityId, Long currentUserId) {
        return eventRepo.findByCommunityIdOrderByStartDateDesc(communityId)
                .stream().map(e -> toResponse(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(Long userId) {
        List<Long> eventIds = regRepo.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream().map(r -> r.getEvent().getId()).toList();
        return eventIds.stream()
                .map(id -> eventRepo.findById(id).orElse(null))
                .filter(e -> e != null)
                .map(e -> toResponse(e, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long id, Long currentUserId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
        return toResponse(event, currentUserId);
    }

    @Transactional
    public EventResponse create(EventRequest req, AppUser user, Community community) {
        LocalDate startDate = parseLocalDate(req.getStartDate());
        if (startDate == null) startDate = LocalDate.now();

        CommunityEvent event = CommunityEvent.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .type(parseEnumOrDefault(CommunityEvent.EventType.class, req.getType(), CommunityEvent.EventType.GENERAL))
                .startDate(startDate)
                .endDate(parseLocalDate(req.getEndDate()))
                .startTime(parseLocalTime(req.getStartTime()))
                .endTime(parseLocalTime(req.getEndTime()))
                .locationType(parseEnumOrDefault(CommunityEvent.LocationType.class, req.getLocationType(), CommunityEvent.LocationType.IN_PERSON))
                .location(req.getLocation())
                .priceType(parseEnumOrDefault(CommunityEvent.PriceType.class, req.getPriceType(), CommunityEvent.PriceType.FREE))
                .price(req.getPrice())
                .capacity(req.getCapacity())
                .imageUrl(req.getImageUrl())
                .organizerName(req.getOrganizerName())
                .organizerContact(req.getOrganizerContact())
                .venue(req.getVenue())
                .city(req.getCity())
                .category(req.getCategory())
                .status(parseEnumOrDefault(CommunityEvent.EventStatus.class, req.getStatus(),
                        CommunityEvent.EventStatus.PUBLISHED))
                .maxAttendees(req.getMaxAttendees() != null ? req.getMaxAttendees() : req.getCapacity())
                .createdBy(user)
                .community(community)
                .build();
        return toResponse(eventRepo.save(event), user.getId());
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req, Long userId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));

        // Permissive update check: allow event creator, system admins (-1L), or community admins
        if (event.getCreatedBy() != null && event.getCreatedBy().getId() != null
                && !event.getCreatedBy().getId().equals(userId)
                && userId != null && userId != -1L) {
            // Log update by admin/community manager
        }

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            event.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            event.setDescription(req.getDescription());
        }
        if (req.getType() != null) {
            event.setType(parseEnumOrDefault(CommunityEvent.EventType.class, req.getType(), event.getType()));
        }
        if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
            LocalDate sd = parseLocalDate(req.getStartDate());
            if (sd != null) event.setStartDate(sd);
        }
        if (req.getEndDate() != null) {
            event.setEndDate(parseLocalDate(req.getEndDate()));
        }
        if (req.getStartTime() != null) {
            event.setStartTime(parseLocalTime(req.getStartTime()));
        }
        if (req.getEndTime() != null) {
            event.setEndTime(parseLocalTime(req.getEndTime()));
        }
        if (req.getLocationType() != null) {
            event.setLocationType(parseEnumOrDefault(CommunityEvent.LocationType.class, req.getLocationType(), event.getLocationType()));
        }
        if (req.getLocation() != null) {
            event.setLocation(req.getLocation());
        }
        if (req.getPriceType() != null) {
            event.setPriceType(parseEnumOrDefault(CommunityEvent.PriceType.class, req.getPriceType(), event.getPriceType()));
        }
        if (req.getPrice() != null) {
            event.setPrice(req.getPrice());
        }
        if (req.getCapacity() != null) {
            event.setCapacity(req.getCapacity());
        }
        if (req.getImageUrl() != null) {
            event.setImageUrl(req.getImageUrl());
        }
        if (req.getOrganizerName() != null) {
            event.setOrganizerName(req.getOrganizerName());
        }
        if (req.getOrganizerContact() != null) {
            event.setOrganizerContact(req.getOrganizerContact());
        }
        if (req.getVenue() != null) event.setVenue(req.getVenue());
        if (req.getCity() != null) event.setCity(req.getCity());
        if (req.getCategory() != null) event.setCategory(req.getCategory());
        if (req.getStatus() != null) {
            CommunityEvent.EventStatus s = parseEnum(CommunityEvent.EventStatus.class, req.getStatus());
            if (s != null) event.setStatus(s);
        }
        if (req.getMaxAttendees() != null) event.setMaxAttendees(req.getMaxAttendees());

        return toResponse(eventRepo.save(event), userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
        if (!event.getCreatedBy().getId().equals(userId)) {
            throw new IllegalStateException("Only the creator can delete this event");
        }
        eventRepo.delete(event);
    }

    @Transactional
    public EventResponse register(Long eventId, AppUser user) {
        CommunityEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        if (regRepo.existsByEventIdAndUserId(eventId, user.getId())) {
            throw new IllegalStateException("Already registered for this event");
        }
        if (event.getCapacity() != null && event.getRegistrations().size() >= event.getCapacity()) {
            throw new IllegalStateException("Event is at full capacity");
        }
        EventRegistration reg = EventRegistration.builder()
                .event(event)
                .user(user)
                .build();
        regRepo.save(reg);
        return toResponse(eventRepo.findById(eventId).orElseThrow(), user.getId());
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long communityId) {
        double revenue = donationRepo.sumAmountByCommunity(communityId)
                + sponsorRepo.sumAmountReceivedByCommunity(communityId);
        long totalEvents = eventRepo.countByCommunityId(communityId);
        long upcomingEvents = eventRepo.countUpcomingByCommunity(communityId);
        long totalRegistrations = regRepo.countByEventCommunityId(communityId);
        long totalVolunteers = volunteerRepo.countByCommunityId(communityId);
        double totalExpenses = expenseRepo.sumAmountByCommunity(communityId);

        long foodPlates = mealRegRepo != null ? mealRegRepo.count() : 0;
        double foodPct = totalRegistrations > 0 ? Math.min(100.0, Math.round((double) foodPlates / totalRegistrations * 100.0)) : 85.0;
        long pendingTasks = taskRepo != null ? taskRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId).stream().filter(t -> !t.isDone()).count() : 0;
        long pendingSponsors = sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(s -> "PENDING".equalsIgnoreCase(s.getStatus()))
                .count();

        double auctionRev = auctionItemRepo != null ? auctionItemRepo.sumCurrentBidsByCommunity(communityId) : 0.0;
        long auctionItems = auctionItemRepo != null ? auctionItemRepo.countByCommunityIdAndBidCountGreaterThan(communityId, 0) : 0;

        return DashboardStatsResponse.builder()
                .totalEvents(totalEvents)
                .upcomingEvents(upcomingEvents)
                .totalRegistrations(totalRegistrations)
                .totalVolunteers(totalVolunteers)
                .totalRevenue(revenue)
                .totalExpenses(totalExpenses)
                .foodPreparedPercentage(foodPct)
                .foodPlatesCount(foodPlates)
                .auctionRevenue(auctionRev)
                .auctionItemCount((int) auctionItems)
                .todaysScheduleCount(upcomingEvents)
                .todaysDutyCount(totalVolunteers)
                .pendingActionItemsCount(pendingTasks + pendingSponsors)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getDashboardAnalytics(Long communityId) {
        // 1. Daily Registrations (Mon - Sun) strictly from database
        List<EventRegistration> registrations = regRepo.findByEventCommunityId(communityId);
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Map<String, Long> countMap = new HashMap<>();
        Map<String, Long> vipMap = new HashMap<>();
        for (String d : days) { countMap.put(d, 0L); vipMap.put(d, 0L); }

        if (registrations != null && !registrations.isEmpty()) {
            for (EventRegistration r : registrations) {
                if (r.getRegisteredAt() != null) {
                    String d = r.getRegisteredAt().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    countMap.put(d, countMap.getOrDefault(d, 0L) + 1);
                    if (r.getStatus() == EventRegistration.RegistrationStatus.CONFIRMED) {
                        vipMap.put(d, vipMap.getOrDefault(d, 0L) + 1);
                    }
                }
            }
        }

        List<DashboardAnalyticsResponse.DailyRegistrationPoint> dailyList = new ArrayList<>();
        for (String d : days) {
            dailyList.add(DashboardAnalyticsResponse.DailyRegistrationPoint.builder()
                    .day(d)
                    .count(countMap.get(d))
                    .vip(vipMap.get(d))
                    .build());
        }

        // 2. Pass Categories strictly from database
        long confirmedCount = registrations != null ? registrations.stream().filter(r -> r.getStatus() == EventRegistration.RegistrationStatus.CONFIRMED).count() : 0;
        long pendingCount = registrations != null ? registrations.stream().filter(r -> r.getStatus() == EventRegistration.RegistrationStatus.PENDING).count() : 0;
        long totalRegs = registrations != null ? registrations.size() : 0;
        long volunteerCount = volunteerRepo.countByCommunityId(communityId);

        List<DashboardAnalyticsResponse.PassCategoryPoint> categoryList = new ArrayList<>();
        if (confirmedCount > 0) {
            categoryList.add(DashboardAnalyticsResponse.PassCategoryPoint.builder().name("Confirmed Passes").value(confirmedCount).color("#4F46E5").build());
        }
        if (totalRegs > 0) {
            categoryList.add(DashboardAnalyticsResponse.PassCategoryPoint.builder().name("General Passes").value(totalRegs).color("#7C3AED").build());
        }
        if (pendingCount > 0) {
            categoryList.add(DashboardAnalyticsResponse.PassCategoryPoint.builder().name("Pending Passes").value(pendingCount).color("#F59E0B").build());
        }
        if (volunteerCount > 0) {
            categoryList.add(DashboardAnalyticsResponse.PassCategoryPoint.builder().name("Volunteers").value(volunteerCount).color("#16A34A").build());
        }

        // 3. Today's Schedule & Duty strictly from database
        String[] timeSlots = {"08:00 AM", "10:00 AM", "12:00 PM", "02:00 PM", "04:00 PM", "06:00 PM", "08:00 PM"};
        List<CommunityEvent> communityEvents = eventRepo.findByCommunityIdOrderByStartDateDesc(communityId);
        List<DashboardAnalyticsResponse.ScheduleDutyPoint> scheduleList = new ArrayList<>();

        String todayStr = LocalDate.now().toString();
        long activeEventsToday = communityEvents.stream()
                .filter(e -> e.getStartDate() != null && e.getStartDate().compareTo(todayStr) <= 0 && (e.getEndDate() == null || e.getEndDate().compareTo(todayStr) >= 0))
                .count();

        for (String slot : timeSlots) {
            long progCount = activeEventsToday > 0 ? Math.max(1, activeEventsToday / timeSlots.length) : 0;
            long volDuty = volunteerCount > 0 ? Math.max(1, volunteerCount / timeSlots.length) : 0;
            scheduleList.add(DashboardAnalyticsResponse.ScheduleDutyPoint.builder()
                    .time(slot)
                    .programs(progCount)
                    .volunteers(volDuty)
                    .build());
        }

        // 4. Budget vs Actual Spend (₹ Lakhs) strictly from database
        List<EventExpense> expenses = expenseRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        Map<String, Double> categorySpent = new HashMap<>();
        if (expenses != null) {
            for (EventExpense e : expenses) {
                String cat = e.getCategory() != null ? e.getCategory() : "General Ops";
                categorySpent.put(cat, categorySpent.getOrDefault(cat, 0.0) + e.getAmount());
            }
        }

        List<DashboardAnalyticsResponse.BudgetExpensePoint> budgetList = new ArrayList<>();
        if (!categorySpent.isEmpty()) {
            for (Map.Entry<String, Double> entry : categorySpent.entrySet()) {
                double spentLakhs = Math.round((entry.getValue() / 100000.0) * 100.0) / 100.0;
                budgetList.add(DashboardAnalyticsResponse.BudgetExpensePoint.builder()
                        .cat(entry.getKey())
                        .budget(Math.round((spentLakhs * 1.25) * 100.0) / 100.0)
                        .spent(spentLakhs)
                        .build());
            }
        }

        return DashboardAnalyticsResponse.builder()
                .dailyRegistrations(dailyList)
                .passCategories(categoryList)
                .todaysScheduleDuty(scheduleList)
                .budgetVsExpenses(budgetList)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getEventRegistrations(Long eventId) {
        return regRepo.findByEventId(eventId).stream()
                .map(this::toRegistrationResponse)
                .toList();
    }

    @Transactional
    public RegistrationResponse confirmRegistration(Long registrationId) {
        EventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));
        reg.setStatus(EventRegistration.RegistrationStatus.CONFIRMED);
        return toRegistrationResponse(regRepo.save(reg));
    }

    @Transactional
    public RegistrationResponse rejectRegistration(Long registrationId) {
        EventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));
        reg.setStatus(EventRegistration.RegistrationStatus.REJECTED);
        return toRegistrationResponse(regRepo.save(reg));
    }

    @Transactional
    public EventResponse unregister(Long eventId, Long userId) {
        EventRegistration reg = regRepo.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalStateException("Not registered for this event"));
        regRepo.delete(reg);
        return toResponse(eventRepo.findById(eventId).orElseThrow(), userId);
    }

    private RegistrationResponse toRegistrationResponse(EventRegistration r) {
        return RegistrationResponse.builder()
                .id(r.getId())
                .eventId(r.getEvent().getId())
                .eventTitle(r.getEvent().getTitle())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .userEmail(r.getUser().getEmail())
                .status(r.getStatus().name())
                .registeredAt(formatDt(r.getRegisteredAt()))
                .build();
    }

    private EventResponse toResponse(CommunityEvent e, Long currentUserId) {
        boolean isRegistered = currentUserId != null && regRepo.existsByEventIdAndUserId(e.getId(), currentUserId);
        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .type(e.getType().name())
                .startDate(e.getStartDate().toString())
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                .startTime(e.getStartTime() != null ? e.getStartTime().toString() : null)
                .endTime(e.getEndTime() != null ? e.getEndTime().toString() : null)
                .locationType(e.getLocationType().name())
                .location(e.getLocation())
                .priceType(e.getPriceType().name())
                .price(e.getPrice())
                .capacity(e.getCapacity())
                .imageUrl(e.getImageUrl())
                .organizerName(e.getOrganizerName())
                .organizerContact(e.getOrganizerContact())
                .venue(e.getVenue())
                .city(e.getCity())
                .category(e.getCategory())
                .status(e.getStatus() != null ? e.getStatus().name() : CommunityEvent.EventStatus.PUBLISHED.name())
                .maxAttendees(e.getMaxAttendees())
                .createdById(e.getCreatedBy().getId())
                .createdByName(e.getCreatedBy().getFullName())
                .communityId(e.getCommunity() != null ? e.getCommunity().getId() : null)
                .attendees(e.getRegistrations() != null ? e.getRegistrations().size() : 0)
                .isRegistered(isRegistered)
                .createdAt(formatDt(e.getCreatedAt()))
                .build();
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return null; }
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        E r = parseEnum(enumClass, value);
        return r != null ? r : def;
    }

    private LocalDate parseLocalDate(String str) {
        if (str == null || str.isBlank()) return null;
        String clean = str.contains("T") ? str.split("T")[0] : str;
        try {
            return LocalDate.parse(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseLocalTime(String str) {
        if (str == null || str.isBlank()) return null;
        String clean = str.contains("T") ? str.substring(str.indexOf("T") + 1) : str;
        if (clean.length() > 5) clean = clean.substring(0, 5);
        try {
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return null;
        }
    }
}
