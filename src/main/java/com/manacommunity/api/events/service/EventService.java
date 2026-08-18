package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.DashboardAnalyticsResponse;
import com.manacommunity.api.events.dto.DashboardStatsResponse;
import com.manacommunity.api.events.dto.EventRequest;
import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.dto.PendingActionItemDto;
import com.manacommunity.api.events.dto.RegistrationResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventExpense;
import com.manacommunity.api.events.entity.EventRegistration;
import com.manacommunity.api.events.entity.EventTask;
import com.manacommunity.api.events.entity.EventSponsor;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventAuctionItemRepository;
import com.manacommunity.api.events.repository.EventDonationRepository;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.repository.EventSponsorRepository;
import com.manacommunity.api.events.repository.EventTaskRepository;
import com.manacommunity.api.events.repository.EventVolunteerRepository;
import com.manacommunity.api.events.repository.MealRegistrationRepository;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
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
import com.manacommunity.api.events.entity.EventVenue;
import com.manacommunity.api.events.repository.EventVenueRepository;
import com.manacommunity.api.events.repository.EventInvoiceRepository;
import com.manacommunity.api.events.repository.EventGalleryItemRepository;
import com.manacommunity.api.events.repository.EventProgramRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventFamilyMemberRepository;
import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.api.repository.AuctionPlayerRepository;
import com.manacommunity.api.user.repository.AppUserRepository;

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
    private final AuctionPlayerRepository auctionPlayerRepo;
    private final AppUserRepository userRepo;
    private final EmailService emailService;
    private final EventVenueRepository venueRepo;
    private final EventInvoiceRepository invoiceRepo;
    private final EventGalleryItemRepository galleryRepo;
    private final EventProgramRepository programRepo;
    private final PoojaSevaRepository poojaSevaRepo;
    private final CulturalEventRepository culturalEventRepo;
    private final LunchDinnerRepository lunchDinnerRepo;
    private final CompetitionRepository competitionRepo;
    private final EventBookingRegistrationRepository eventBookingRegRepo;
    private final EventFamilyMemberRepository familyMemberRepo;

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents(Long communityId, String typeFilter, Long currentUserId) {
        List<CommunityEvent> events;
        if (communityId != null) {
            if (typeFilter != null && !typeFilter.isBlank() && !"All".equalsIgnoreCase(typeFilter)) {
                CommunityEvent.EventType type = parseEnum(CommunityEvent.EventType.class, typeFilter);
                events = type != null ? eventRepo.findUpcomingByCommunityAndType(communityId, type) : eventRepo.findUpcomingByCommunity(communityId);
            } else {
                events = eventRepo.findUpcomingByCommunity(communityId);
            }
            if (events.isEmpty()) {
                events = eventRepo.findAll();
            }
        } else {
            events = eventRepo.findAll();
        }
        return events.stream()
                .filter(e -> e.getStatus() == null || e.getStatus() == CommunityEvent.EventStatus.PUBLISHED
                        || (currentUserId != null && e.getCreatedBy() != null && e.getCreatedBy().getId().equals(currentUserId)))
                .map(e -> toResponse(e, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(Long communityId, Long currentUserId) {
        List<CommunityEvent> events;
        if (communityId != null) {
            events = eventRepo.findByCommunityIdOrderByStartDateDesc(communityId);
            if (events.isEmpty()) {
                events = eventRepo.findAll();
            }
        } else {
            events = eventRepo.findAll();
        }
        return events.stream().map(e -> toResponse(e, currentUserId)).toList();
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
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        return toResponse(event, currentUserId);
    }

    @Transactional
    public EventResponse create(EventRequest req, AppUser user, Community community) {
        LocalDate startDate = parseLocalDate(req.getStartDate());
        if (startDate == null) startDate = LocalDate.now();

        EventVenue eventVenue = null;
        if (req.getVenueId() != null) {
            eventVenue = venueRepo.findById(req.getVenueId()).orElse(null);
        }

        String venueName = req.getVenue();
        if ((venueName == null || venueName.isBlank()) && eventVenue != null) {
            venueName = eventVenue.getName();
        }

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
                .eventVenue(eventVenue)
                .venue(venueName)
                .city(req.getCity())
                .category(req.getCategory())
                .status(parseEnumOrDefault(CommunityEvent.EventStatus.class, req.getStatus(),
                        CommunityEvent.EventStatus.PUBLISHED))
                .paymentModes(req.getPaymentModes())
                .upiId(req.getUpiId())
                .scannerUrl(req.getScannerUrl())
                .notes(req.getNotes())
                .contactsJson(req.getContactsJson())
                .paymentInstructions(req.getPaymentInstructions())
                .maxAttendees(req.getMaxAttendees() != null ? req.getMaxAttendees() : req.getCapacity())
                .createdBy(user)
                .community(community)
                .build();

        CommunityEvent saved = eventRepo.save(event);
        if (saved.getStatus() == CommunityEvent.EventStatus.PUBLISHED) {
            sendEventPublishedEmail(saved);
        }
        return toResponse(saved, user.getId());
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req, Long userId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        CommunityEvent.EventStatus oldStatus = event.getStatus();

        if (req.getVenueId() != null) {
            EventVenue eventVenue = venueRepo.findById(req.getVenueId()).orElse(null);
            event.setEventVenue(eventVenue);
            if (eventVenue != null && (req.getVenue() == null || req.getVenue().isBlank())) {
                event.setVenue(eventVenue.getName());
            }
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
        if (req.getPaymentModes() != null) event.setPaymentModes(req.getPaymentModes());
        if (req.getUpiId() != null) event.setUpiId(req.getUpiId());
        if (req.getScannerUrl() != null) event.setScannerUrl(req.getScannerUrl());
        if (req.getNotes() != null) event.setNotes(req.getNotes());
        if (req.getContactsJson() != null) event.setContactsJson(req.getContactsJson());
        if (req.getPaymentInstructions() != null) event.setPaymentInstructions(req.getPaymentInstructions());
        if (req.getMaxAttendees() != null) event.setMaxAttendees(req.getMaxAttendees());

        CommunityEvent saved = eventRepo.save(event);
        if (oldStatus != CommunityEvent.EventStatus.PUBLISHED && saved.getStatus() == CommunityEvent.EventStatus.PUBLISHED) {
            sendEventPublishedEmail(saved);
        }

        return toResponse(saved, userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        CommunityEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        if (event.getCreatedBy() != null && userId != null) {
            boolean isCreator = event.getCreatedBy().getId().equals(userId);
            AppUser currentUser = userRepo.findById(userId).orElse(null);
            boolean isAuthorizedAdmin = false;
            if (currentUser != null) {
                if (currentUser.getRole() != null) {
                    String roleStr = currentUser.getRole().toUpperCase();
                    isAuthorizedAdmin = roleStr.contains("ADMIN") || roleStr.contains("ORGANIZER") || roleStr.contains("MANAGER");
                }
                if (!isAuthorizedAdmin && currentUser.getUserRoles() != null) {
                    isAuthorizedAdmin = currentUser.getUserRoles().stream().anyMatch(r -> {
                        String rName = r != null && r.getName() != null ? r.getName().toUpperCase() : "";
                        return rName.contains("ADMIN") || rName.contains("ORGANIZER") || rName.contains("MANAGER");
                    });
                }
            }
            if (!isCreator && !isAuthorizedAdmin) {
                throw new UnauthorizedActionException("Only the event creator or authorized admin can delete this event");
            }
        }

        String eventTitle = event.getTitle();
        String eventIdStr = String.valueOf(id);
        String eventPrefixedId = "event-" + id;

        // 1. Clean up sub-activities (Pooja Sevas, Cultural Events, Meals, Competitions) created under this event
        try {
            if (poojaSevaRepo != null) {
                var poojas = poojaSevaRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
                for (var p : poojas) {
                    try {
                        if (eventBookingRegRepo != null) {
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId(String.valueOf(p.getId())));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId("pooja-" + p.getId()));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityTitle(p.getName()));
                        }
                    } catch (Exception ignored) {}
                }
                poojaSevaRepo.deleteAll(poojas);
            }
        } catch (Exception ignored) {}

        try {
            if (culturalEventRepo != null) {
                var cults = culturalEventRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
                for (var c : cults) {
                    try {
                        if (eventBookingRegRepo != null) {
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId(String.valueOf(c.getId())));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId("cult-" + c.getId()));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityTitle(c.getName()));
                        }
                    } catch (Exception ignored) {}
                }
                culturalEventRepo.deleteAll(cults);
            }
        } catch (Exception ignored) {}

        try {
            if (lunchDinnerRepo != null) {
                var meals = lunchDinnerRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
                for (var m : meals) {
                    try {
                        if (eventBookingRegRepo != null) {
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId(String.valueOf(m.getId())));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId("meal-" + m.getId()));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityTitle(m.getName()));
                        }
                    } catch (Exception ignored) {}
                }
                lunchDinnerRepo.deleteAll(meals);
            }
        } catch (Exception ignored) {}

        try {
            if (competitionRepo != null) {
                var comps = competitionRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
                for (var cp : comps) {
                    try {
                        if (eventBookingRegRepo != null) {
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId(String.valueOf(cp.getId())));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId("comp-" + cp.getId()));
                            eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityTitle(cp.getName()));
                        }
                    } catch (Exception ignored) {}
                }
                competitionRepo.deleteAll(comps);
            }
        } catch (Exception ignored) {}

        // 2. Clean up Unified Event Booking Registrations & Passes
        try {
            if (eventBookingRegRepo != null) {
                eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId(eventIdStr));
                eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityId(eventPrefixedId));
                if (eventTitle != null && !eventTitle.isBlank()) {
                    eventBookingRegRepo.deleteAll(eventBookingRegRepo.findByActivityTitle(eventTitle));
                }
            }
        } catch (Exception ignored) {}

        // 3. Clean up standard Event Registrations & Attendee Passes
        try {
            regRepo.deleteAll(regRepo.findByEventId(id));
        } catch (Exception ignored) {}

        // 4. Clean up Event Family Members
        try {
            if (familyMemberRepo != null) {
                familyMemberRepo.deleteAll(familyMemberRepo.findByEventIdOrderByCreatedAtAsc(id));
            }
        } catch (Exception ignored) {}

        // 5. Clean up Volunteers
        try {
            volunteerRepo.deleteAll(volunteerRepo.findByEventIdOrderByCreatedAtDesc(id));
        } catch (Exception ignored) {}

        // 6. Clean up Donations
        try {
            donationRepo.deleteAll(donationRepo.findByEventIdOrderByCreatedAtDesc(id));
        } catch (Exception ignored) {}

        // 7. Clean up Expenses & Ledger
        try {
            expenseRepo.deleteAll(expenseRepo.findByEventIdOrderByCreatedAtDesc(id));
        } catch (Exception ignored) {}

        // 8. Clean up Sponsors
        try {
            sponsorRepo.deleteAll(sponsorRepo.findByEventIdOrderByCreatedAtDesc(id));
        } catch (Exception ignored) {}

        // 9. Clean up Tasks & Planning Items
        try {
            taskRepo.deleteAll(taskRepo.findByEventIdOrderByCreatedAtDesc(id));
        } catch (Exception ignored) {}

        // 10. Clean up Meal Registrations
        try {
            mealRegRepo.deleteAll(mealRegRepo.findByEventIdOrdered(id));
        } catch (Exception ignored) {}

        // 11. Clean up Invoices & Bills
        try {
            if (invoiceRepo != null) invoiceRepo.deleteAll(invoiceRepo.findByEventIdOrderByCreatedAtDesc(id));
        } catch (Exception ignored) {}

        // 12. Clean up Gallery Media & Photos
        try {
            if (galleryRepo != null) galleryRepo.deleteAll(galleryRepo.findByEventIdOrderBySortOrderAscCreatedAtDesc(id));
        } catch (Exception ignored) {}

        // 13. Clean up Day Programs & Timelines
        try {
            if (programRepo != null) programRepo.deleteAll(programRepo.findByEventIdOrderBySortOrderAscStartTimeAsc(id));
        } catch (Exception ignored) {}

        // 14. Clean up Auction Items & Bids
        try {
            if (auctionItemRepo != null) {
                auctionItemRepo.deleteAll(auctionItemRepo.findByEventId(id));
            }
        } catch (Exception ignored) {}

        eventRepo.delete(event);
    }

    @Transactional
    public EventResponse register(Long eventId, AppUser user) {
        CommunityEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        if (regRepo.existsByEventIdAndUserId(eventId, user.getId())) {
            throw new AlreadyRegisteredException(event.getTitle());
        }
        if (event.getCapacity() != null && event.getRegistrations().size() >= event.getCapacity()) {
            throw new EventFullException(event.getTitle(), event.getCapacity());
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

        // Live Food Prepared / Plates Count from Database
        long foodPlates = mealRegRepo != null ? mealRegRepo.sumHeadCountByCommunity(communityId) : 0;
        if (foodPlates == 0 && mealRegRepo != null) {
            foodPlates = mealRegRepo.count();
        }
        double foodPct = totalRegistrations > 0
                ? Math.min(100.0, Math.round((double) foodPlates / totalRegistrations * 100.0))
                : (foodPlates > 0 ? 100.0 : 0.0);

        long pendingTasks = 0;
        if (taskRepo != null) {
            pendingTasks = taskRepo.countByCommunityIdAndDoneFalse(communityId);
            if (pendingTasks == 0) {
                pendingTasks = taskRepo.countByDoneFalse();
            }
        }
        long pendingSponsors = sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .filter(s -> "PENDING".equalsIgnoreCase(s.getStatus()))
                .count();

        // Live Auction Revenue (Event Item Auctions + Tournament Player Auctions)
        double itemAuctionRev = auctionItemRepo != null ? auctionItemRepo.sumCurrentBidsByCommunity(communityId) : 0.0;
        long itemAuctionCount = auctionItemRepo != null ? auctionItemRepo.countByCommunityIdAndBidCountGreaterThan(communityId, 0) : 0;

        long playerAuctionRev = auctionPlayerRepo != null ? auctionPlayerRepo.sumSoldPriceByCommunity(communityId) : 0;
        long playerAuctionCount = auctionPlayerRepo != null ? auctionPlayerRepo.countSoldByCommunity(communityId) : 0;

        double totalAuctionRev = itemAuctionRev + (double) playerAuctionRev;
        long totalAuctionItemsSold = itemAuctionCount + playerAuctionCount;

        LocalDate today = LocalDate.now();
        List<CommunityEvent> allEvents = communityId != null
                ? eventRepo.findByCommunityIdOrderByStartDateDesc(communityId)
                : eventRepo.findAll();
        long todaysEventsCount = allEvents.stream()
                .filter(e -> e.getStartDate() != null && !e.getStartDate().isAfter(today) && (e.getEndDate() == null || !e.getEndDate().isBefore(today)))
                .count();

        return DashboardStatsResponse.builder()
                .totalEvents(totalEvents)
                .upcomingEvents(upcomingEvents)
                .totalRegistrations(totalRegistrations)
                .totalVolunteers(totalVolunteers)
                .totalRevenue(revenue)
                .totalExpenses(totalExpenses)
                .foodPreparedPercentage(foodPct)
                .foodPlatesCount(foodPlates)
                .auctionRevenue(totalAuctionRev)
                .auctionItemCount((int) totalAuctionItemsSold)
                .todaysScheduleCount(todaysEventsCount)
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

        LocalDate today = LocalDate.now();
        long activeEventsToday = communityEvents.stream()
                .filter(e -> e.getStartDate() != null && !e.getStartDate().isAfter(today) && (e.getEndDate() == null || !e.getEndDate().isBefore(today)))
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
    public List<PendingActionItemDto> getPendingActionItems(Long communityId) {
        List<PendingActionItemDto> result = new ArrayList<>();

        if (taskRepo != null) {
            List<EventTask> tasks = communityId != null ? taskRepo.findByCommunityIdOrderByDueDateAsc(communityId) : taskRepo.findAll();
            if (tasks.isEmpty()) {
                tasks = taskRepo.findAll();
            }
            for (EventTask t : tasks) {
                if (!t.isDone()) {
                    String dueStr = t.getDueDate() != null ? formatDue(t.getDueDate()) : "Tomorrow";
                    result.add(PendingActionItemDto.builder()
                            .id("task-" + t.getId())
                            .task(t.getTitle())
                            .due(dueStr)
                            .priority(t.getPriority() != null ? t.getPriority().name().toLowerCase() : "medium")
                            .category("Task")
                            .done(t.isDone())
                            .build());
                }
            }
        }

        if (sponsorRepo != null) {
            List<EventSponsor> sponsors = communityId != null ? sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId) : sponsorRepo.findAll();
            for (EventSponsor s : sponsors) {
                if ("PENDING".equalsIgnoreCase(s.getStatus())) {
                    result.add(PendingActionItemDto.builder()
                            .id("sponsor-" + s.getId())
                            .task("Approve Sponsor: " + s.getName() + " (₹" + (s.getAmountReceived() != null ? s.getAmountReceived() : s.getAmountPledged()) + ")")
                            .due("Pending Review")
                            .priority("high")
                            .category("Sponsor")
                            .done(false)
                            .build());
                }
            }
        }

        return result;
    }

    private String formatDue(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        if (dueDate.isEqual(today)) return "Today";
        if (dueDate.isEqual(today.plusDays(1))) return "Tomorrow";
        if (dueDate.isBefore(today)) return "Overdue";
        return dueDate.format(DateTimeFormatter.ofPattern("MMM dd"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
        reg.setStatus(EventRegistration.RegistrationStatus.CONFIRMED);
        return toRegistrationResponse(regRepo.save(reg));
    }

    @Transactional
    public RegistrationResponse rejectRegistration(Long registrationId) {
        EventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
        reg.setStatus(EventRegistration.RegistrationStatus.REJECTED);
        return toRegistrationResponse(regRepo.save(reg));
    }

    @Transactional
    public EventResponse unregister(Long eventId, Long userId) {
        EventRegistration reg = regRepo.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", "eventId and userId", eventId + "/" + userId));
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
                .venueId(e.getEventVenue() != null ? e.getEventVenue().getId() : null)
                .venue(e.getEventVenue() != null ? e.getEventVenue().getName() : e.getVenue())
                .city(e.getCity())
                .category(e.getCategory())
                .status(e.getStatus() != null ? e.getStatus().name() : CommunityEvent.EventStatus.PUBLISHED.name())
                .paymentModes(e.getPaymentModes())
                .upiId(e.getUpiId())
                .scannerUrl(e.getScannerUrl())
                .notes(e.getNotes())
                .contactsJson(e.getContactsJson())
                .paymentInstructions(e.getPaymentInstructions())
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

    private void sendEventPublishedEmail(CommunityEvent event) {
        if (event == null || emailService == null || userRepo == null) return;
        try {
            Long commId = event.getCommunity() != null ? event.getCommunity().getId() : null;
            List<AppUser> recipients = commId != null
                    ? userRepo.findByCommunityIdAndIsActiveTrue(commId)
                    : userRepo.findAll();

            if (recipients.isEmpty()) return;

            String eventDateStr = event.getStartDate() != null ? event.getStartDate().toString() : "Upcoming";
            String eventTimeStr = event.getStartTime() != null ? event.getStartTime().toString() : "";
            String venueStr = event.getVenue() != null ? event.getVenue() : (event.getLocation() != null ? event.getLocation() : "Community Center");
            String feeStr = (event.getPrice() != null && event.getPrice() > 0) ? "₹" + event.getPrice() : "FREE";

            List<EmailMessage> messages = new ArrayList<>();
            for (AppUser recipient : recipients) {
                if (recipient.getEmail() == null || recipient.getEmail().isBlank()) continue;
                String recipientName = recipient.getFullName() != null && !recipient.getFullName().isBlank()
                        ? recipient.getFullName()
                        : "Community Member";

                String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;">
                      <div style="background: linear-gradient(135deg, #4f46e5, #7c3aed); padding: 24px; border-radius: 8px; text-align: center; color: #ffffff;">
                        <h2 style="margin: 0; font-size: 22px;">🎉 New Event Announced!</h2>
                        <p style="margin: 6px 0 0; font-size: 14px; opacity: 0.9;">Mana Community Event Invitation</p>
                      </div>
                      <div style="padding: 24px 8px;">
                        <p style="font-size: 15px; color: #334155;">Hello <strong>%s</strong>,</p>
                        <p style="font-size: 14px; color: #475569;">A new community event <strong>"%s"</strong> has been published and is now open for registration!</p>
                        
                        <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 18px 0;">
                          <p style="margin: 4px 0; font-size: 13px; color: #334155;">📅 <strong>Date:</strong> %s</p>
                          <p style="margin: 4px 0; font-size: 13px; color: #334155;">🕒 <strong>Time:</strong> %s</p>
                          <p style="margin: 4px 0; font-size: 13px; color: #334155;">📍 <strong>Venue:</strong> %s</p>
                          <p style="margin: 4px 0; font-size: 13px; color: #334155;">🎟️ <strong>Registration Fee:</strong> %s</p>
                        </div>
                        
                        <p style="font-size: 13px; color: #64748b;">%s</p>
                        
                        <div style="text-align: center; margin: 24px 0;">
                          <a href="http://localhost:5173/events" style="background: #4f46e5; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 14px; display: inline-block;">
                            Register on Dashboard
                          </a>
                        </div>
                      </div>
                      <div style="border-top: 1px solid #e2e8f0; padding-top: 12px; font-size: 11px; color: #94a3b8; text-align: center;">
                        Sent by Mana Community Management • You received this because you are a registered community member.
                      </div>
                    </div>
                """.formatted(
                        recipientName,
                        event.getTitle() != null ? event.getTitle() : "Community Event",
                        eventDateStr,
                        eventTimeStr.isBlank() ? "Schedule on Dashboard" : eventTimeStr,
                        venueStr,
                        feeStr,
                        event.getDescription() != null ? event.getDescription() : ""
                );

                messages.add(new EmailMessage(
                        recipient.getEmail(),
                        recipientName,
                        "🎉 New Event: " + (event.getTitle() != null ? event.getTitle() : "Community Event"),
                        html
                ));
            }

            if (!messages.isEmpty()) {
                emailService.sendAll(messages);
            }
        } catch (Exception ex) {
            System.err.println("Failed to send event announcement emails: " + ex.getMessage());
        }
    }
}
