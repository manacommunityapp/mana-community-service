package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventCompetition;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.EventLunchDinner;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.entity.EventTicketCategory;
import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.events.enums.RegistrationSource;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.entity.EventMealRegistration;
import com.manacommunity.api.events.repository.EventMealRegistrationRepository;
import com.manacommunity.api.events.repository.EventPoojaSlotReservationRepository;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.EventTicketCategoryRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.EventBookingRegistrationService;
import com.manacommunity.api.events.service.PoojaSlotReservationService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.RegistrationClosedException;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class EventBookingRegistrationServiceImpl implements EventBookingRegistrationService {

    private final EventBookingRegistrationRepository repository;
    private final EventPoojaUserRegistrationRepository poojaUserRegRepo;
    private final CommunityRepository communityRepository;
    private final PoojaSevaRepository poojaSevaRepository;
    private final LunchDinnerRepository lunchDinnerRepository;
    private final CompetitionRepository competitionRepository;
    private final CulturalEventRepository culturalEventRepository;
    private final EventCommunityRepository communityEventRepository;
    private final EventTicketCategoryRepository ticketCategoryRepository;
    private final AppUserRepository appUserRepository;
    private final PoojaSlotReservationService poojaSlotReservationService;
    private final EventPoojaSlotReservationRepository poojaSlotReservationRepository;
    private final EventMealRegistrationRepository mealRegistrationRepository;

    public EventBookingRegistrationServiceImpl(
            EventBookingRegistrationRepository repository,
            EventPoojaUserRegistrationRepository poojaUserRegRepo,
            CommunityRepository communityRepository,
            PoojaSevaRepository poojaSevaRepository,
            LunchDinnerRepository lunchDinnerRepository,
            CompetitionRepository competitionRepository,
            CulturalEventRepository culturalEventRepository,
            EventCommunityRepository communityEventRepository,
            EventTicketCategoryRepository ticketCategoryRepository,
            AppUserRepository appUserRepository,
            PoojaSlotReservationService poojaSlotReservationService,
            EventPoojaSlotReservationRepository poojaSlotReservationRepository,
            EventMealRegistrationRepository mealRegistrationRepository) {
        this.repository = repository;
        this.poojaUserRegRepo = poojaUserRegRepo;
        this.communityRepository = communityRepository;
        this.poojaSevaRepository = poojaSevaRepository;
        this.lunchDinnerRepository = lunchDinnerRepository;
        this.competitionRepository = competitionRepository;
        this.culturalEventRepository = culturalEventRepository;
        this.communityEventRepository = communityEventRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.appUserRepository = appUserRepository;
        this.poojaSlotReservationService = poojaSlotReservationService;
        this.poojaSlotReservationRepository = poojaSlotReservationRepository;
        this.mealRegistrationRepository = mealRegistrationRepository;
    }

    private boolean isUserAdmin(AppUser user) {
        if (user == null) return false;
        return user.hasRole("ADMIN") ||
                user.hasRole("COMMUNITY_ADMIN") ||
                user.hasRole("EVENT_ADMIN") ||
                user.hasRole("SUPER_ADMIN") ||
                user.hasRole("ROLE_ADMIN") ||
                user.hasRole("ROLE_COMMUNITY_ADMIN") ||
                user.hasRole("ROLE_EVENT_ADMIN") ||
                user.hasRole("ROLE_SUPER_ADMIN");
    }

    @Override
    @Transactional
    public EventBookingRegistration createRegistration(EventBookingRegistration registration, AppUser user, Long communityId) {
        return createRegistration(registration, user, communityId, false);
    }

    @Override
    @Transactional
    public EventBookingRegistration createRegistration(EventBookingRegistration registration, AppUser user, Long communityId, boolean adminOverride) {
        boolean isAdmin = isUserAdmin(user) || adminOverride;
        AppUser targetUser = user;

        if (isAdmin) {
            if (registration.getUser() != null && registration.getUser().getId() != null) {
                targetUser = appUserRepository.findById(registration.getUser().getId()).orElse(user);
            }
        }

        if (!isAdmin) {
            Long validatingUserId = (targetUser != null) ? targetUser.getId() : null;
            // When the user pre-reserved a pooja slot, capacity was already enforced atomically
            // (SELECT FOR UPDATE + count check) at reserve time. Skip the racy SELECT COUNT here.
            boolean hasPoojaReservation = registration.getReservationId() != null
                    && registration.getActivityId() != null
                    && registration.getActivityId().startsWith("pooja-");
            if (!hasPoojaReservation) {
                validateCapacityAndDeadline(registration, validatingUserId);
            }
            if (validatingUserId != null && registration.getActivityId() != null && !registration.getActivityId().isBlank()) {
                // One registration per seva (pooja type). Different sevas in the same event are each allowed.
                if (repository.existsByUserIdAndActivityIdAndStatusNot(validatingUserId, registration.getActivityId(), "CANCELLED")) {
                    throw new AlreadyRegisteredException(registration.getActivityTitle() != null ? registration.getActivityTitle() : "this activity",
                            "You are already registered for this activity.");
                }
            }
        }

        Community comm = (targetUser != null && targetUser.getCommunity() != null)
                ? targetUser.getCommunity()
                : (user != null && user.getCommunity() != null)
                        ? user.getCommunity()
                        : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

        registration.setId(null);
        registration.setUser(targetUser);
        registration.setCommunity(comm);

        if (registration.getMainEventId() == null) {
            registration.setMainEventId(resolveMainEventId(registration.getActivityId()));
        }

        if (registration.getRegCode() == null || registration.getRegCode().isBlank()) {
            String prefix = "MNA-2026-REG-";
            if (registration.getCategory() != null) {
                String catCode = registration.getCategory().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                if (catCode.length() > 4) catCode = catCode.substring(0, 4);
                if (!catCode.isEmpty()) prefix = "MNA-2026-" + catCode + "-";
            }
            String rnd = String.format("%06d", new Random().nextInt(900000) + 100000);
            registration.setRegCode(prefix + rnd);
        }

        if (registration.getQrCodeUrl() == null || registration.getQrCodeUrl().isBlank()) {
            registration.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + registration.getRegCode());
        }

        if (registration.getStatus() == null || registration.getStatus().isBlank()) {
            registration.setStatus("CONFIRMED");
        }

        if (registration.getBookingFee() != null) {
            registration.setBookingFee(Math.max(0.0, registration.getBookingFee()));
        } else {
            registration.setBookingFee(0.0);
        }

        if (registration.getPaymentStatus() == null || registration.getPaymentStatus().isBlank()) {
            if (registration.getBookingFee() <= 0.0) {
                registration.setPaymentStatus("FREE");
            } else {
                registration.setPaymentStatus("PAID");
            }
        }

        int computedDevotees = Math.max(1, computeDevoteeCount(registration.getDevoteeCount(), registration.getAttendingDevotees(), registration.getMembersJson()));
        registration.setDevoteeCount(computedDevotees);

        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());

        // Stamp audit / source fields
        if (isAdmin) {
            registration.setRegistrationSource(RegistrationSource.ADMIN);
            if (user != null) registration.setRegisteredBy(user.getId());
        } else {
            registration.setRegistrationSource(RegistrationSource.SELF);
        }
        if (adminOverride) {
            registration.setOverrideUsed(true);
            // overrideReason is caller-supplied; preserve whatever was set on the incoming object
        } else {
            registration.setOverrideUsed(false);
        }

        EventBookingRegistration saved = repository.save(registration);

        // Decrement slots / capacity for the booked activity
        decrementActivitySlots(saved);

        // Sync to dedicated event_pooja_user_registrations table if applicable
        syncToPoojaTableIfApplicable(saved);

        return saved;
    }

    private void validateCapacityAndDeadline(EventBookingRegistration registration, Long userId) {
        String actId = registration.getActivityId();
        if (actId == null || actId.isBlank()) return;

        try {
            if (actId.startsWith("pooja-")) {
                Long id = Long.parseLong(actId.replace("pooja-", ""));
                validatePoojaCapacityAndDeadline(id, registration, userId);
            } else if (actId.startsWith("food-")) {
                Long id = Long.parseLong(actId.replace("food-", ""));
                validateLunchDinnerCapacityAndDeadline(id, registration, userId);
            } else if (actId.startsWith("comp-")) {
                Long id = Long.parseLong(actId.replace("comp-", ""));
                validateCompetitionCapacityAndDeadline(id, registration, userId);
            } else if (actId.startsWith("cultural-") || actId.startsWith("cult-")) {
                Long id = Long.parseLong(actId.replaceAll("^(cultural|cult)-", ""));
                validateCulturalCapacityAndDeadline(id, registration, userId);
            } else if (actId.startsWith("event-")) {
                Long id = Long.parseLong(actId.replace("event-", ""));
                validateEventCapacityAndDeadline(id, registration, userId);
            } else {
                try {
                    Long id = Long.parseLong(actId);
                    validateEventCapacityAndDeadline(id, registration, userId);
                } catch (NumberFormatException ignored) {}
            }
        } catch (AlreadyRegisteredException | EventFullException | RegistrationClosedException ex) {
            throw ex;
        } catch (Exception ignored) {}
    }

    private void validatePoojaCapacityAndDeadline(Long id, EventBookingRegistration registration, Long userId) {
        EventPoojaSeva p = poojaSevaRepository.findById(id).orElse(null);
        if (p == null) return;

        // Check if parent event is cancelled
        if (p.getMainEventId() != null) {
            EventCommunity parent = communityEventRepository.findById(p.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == EventCommunity.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(p.getName(), "Parent event has been cancelled");
            }
        }

        // Only check duplicate for new registrations (not updates)
        boolean isNewRegistration = (registration.getId() == null);

        // Block re-registration for the exact same seva. Different sevas in the same event are each allowed.
        if (isNewRegistration && userId != null
                && repository.existsByUserIdAndActivityIdAndStatusNot(userId, "pooja-" + id, "CANCELLED")) {
            throw new AlreadyRegisteredException(p.getName());
        }

        LocalDate today = LocalDate.now();
        if (p.getEndDate() != null && p.getEndDate().isBefore(today)) {
            throw new RegistrationClosedException(p.getName(), "Pooja date has passed (" + p.getEndDate() + ")");
        } else if (p.getEndDate() == null && p.getDate() != null && p.getDate().isBefore(today)) {
            throw new RegistrationClosedException(p.getName(), "Pooja date has passed (" + p.getDate() + ")");
        }

        int requested = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                ? registration.getDevoteeCount() : 1;

        int maxSlots = p.getSlots() != null && p.getSlots() > 0 ? p.getSlots() : 20;

        List<EventBookingRegistration> existing = repository.findByActivityId("pooja-" + id);
        long bookedSlots = existing.stream()
                .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                .sum();

        if (bookedSlots + requested > maxSlots) {
            throw new EventFullException(p.getName() + " (Capacity: " + maxSlots + " slots, Currently Booked: " + bookedSlots + ")", maxSlots);
        }
    }

    private void validateCulturalCapacityAndDeadline(Long id, EventBookingRegistration registration, Long userId) {
        if (culturalEventRepository == null) return;
        EventCulturalEvent c = culturalEventRepository.findById(id).orElse(null);
        if (c == null) return;

        // Check if parent event is cancelled
        if (c.getMainEventId() != null) {
            EventCommunity parent = communityEventRepository.findById(c.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == EventCommunity.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(c.getName(), "Parent event has been cancelled");
            }
        }

        boolean isNewRegistration = (registration.getId() == null);
        if (isNewRegistration && userId != null) {
            if (repository.existsByUserIdAndActivityIdAndStatusNot(userId, "cultural-" + id, "CANCELLED")
                    || repository.existsByUserIdAndActivityIdAndStatusNot(userId, "cult-" + id, "CANCELLED")) {
                throw new AlreadyRegisteredException(c.getName(), "You are already registered for this cultural event.");
            }
        }

        LocalDate today = LocalDate.now();
        if (c.getDate() != null && c.getDate().isBefore(today)) {
            throw new RegistrationClosedException(c.getName(), "Cultural event date has passed (" + c.getDate() + ")");
        }

        int requested = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                ? registration.getDevoteeCount() : 1;

        int maxEntries = 50; // default max performers/entries per cultural stage segment
        List<EventBookingRegistration> existing = new java.util.ArrayList<>();
        existing.addAll(repository.findByActivityId("cultural-" + id));
        existing.addAll(repository.findByActivityId("cult-" + id));

        long booked = existing.stream()
                .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                .sum();

        if (booked + requested > maxEntries) {
            throw new EventFullException(c.getName() + " (Max entries reached: " + maxEntries + ")", maxEntries);
        }
    }

    private void validateCompetitionCapacityAndDeadline(Long id, EventBookingRegistration registration, Long userId) {
        EventCompetition c = competitionRepository.findById(id).orElse(null);
        if (c == null) return;

        // Check if parent event is cancelled
        if (c.getMainEventId() != null) {
            EventCommunity parent = communityEventRepository.findById(c.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == EventCommunity.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(c.getName(), "Parent event has been cancelled");
            }
        }

        boolean isNewRegistration = (registration.getId() == null);
        if (isNewRegistration && userId != null) {
            if (repository.existsByUserIdAndActivityIdAndStatusNot(userId, "comp-" + id, "CANCELLED")) {
                throw new AlreadyRegisteredException(c.getName(), "You are already registered for this competition.");
            }
        }

        LocalDate today = LocalDate.now();
        if (c.getRegDeadline() != null && today.isAfter(c.getRegDeadline())) {
            throw new RegistrationClosedException(c.getName(), "Registration deadline has passed (" + c.getRegDeadline() + ")");
        }
        if (c.getDate() != null && c.getDate().isBefore(today)) {
            throw new RegistrationClosedException(c.getName(), "EventCompetition date has passed (" + c.getDate() + ")");
        }

        int max = c.getMaxParticipants() != null && c.getMaxParticipants() > 0 ? c.getMaxParticipants() : 50;
        int requested = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                ? registration.getDevoteeCount() : 1;

        List<EventBookingRegistration> existing = repository.findByActivityId("comp-" + id);
        long booked = existing.stream()
                .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                .sum();

        if (booked + requested > max) {
            throw new EventFullException(c.getName() + " (Max Participants: " + max + ", Currently Registered: " + booked + ")", max);
        }
    }

    private void validateLunchDinnerCapacityAndDeadline(Long id, EventBookingRegistration registration, Long userId) {
        EventLunchDinner m = lunchDinnerRepository.findById(id).orElse(null);
        if (m == null) return;

        // Check if parent event is cancelled
        if (m.getMainEventId() != null) {
            EventCommunity parent = communityEventRepository.findById(m.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == EventCommunity.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(m.getName(), "Parent event has been cancelled");
            }
        }

        boolean isNewRegistration = (registration.getId() == null);
        if (isNewRegistration && userId != null) {
            // Check event_booking_registrations (same-path duplicate)
            if (repository.existsByUserIdAndActivityIdAndStatusNot(userId, "food-" + id, "CANCELLED")
                    || repository.existsByUserIdAndActivityIdAndStatusNot(userId, "meal-" + id, "CANCELLED")) {
                throw new AlreadyRegisteredException(m.getName(), "You are already registered for this meal.");
            }
            // Cross-system check: block if user already registered via the meal-preference path
            if (m.getMainEventId() != null && m.getDate() != null && m.getMealType() != null) {
                try {
                    EventMealRegistration.MealType mt =
                            EventMealRegistration.MealType.valueOf(m.getMealType().toUpperCase());
                    if (mealRegistrationRepository.existsByEventIdAndUserIdAndMealDateAndMealType(
                            m.getMainEventId(), userId, m.getDate(), mt)) {
                        throw new AlreadyRegisteredException(m.getName(),
                                "You have already indicated attendance for this meal via meal preferences. Duplicate registration blocked.");
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        LocalDate today = LocalDate.now();
        if (m.getDate() != null && m.getDate().isBefore(today)) {
            throw new RegistrationClosedException(m.getName(), "Meal date has passed (" + m.getDate() + ")");
        }

        int maxPlates = m.getTargetPlates() != null && m.getTargetPlates() > 0 ? m.getTargetPlates() : 500;
        int requested = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                ? registration.getDevoteeCount() : 1;

        List<EventBookingRegistration> existing = new java.util.ArrayList<>();
        existing.addAll(repository.findByActivityId("food-" + id));
        existing.addAll(repository.findByActivityId("meal-" + id));
        long bookedPlates = existing.stream()
                .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                .sum();

        if (bookedPlates + requested > maxPlates) {
            throw new EventFullException(m.getName() + " (Target Plates: " + maxPlates + ", Currently Booked: " + bookedPlates + ")", maxPlates);
        }
    }

    private void validateEventCapacityAndDeadline(Long eventId, EventBookingRegistration registration, Long userId) {
        EventCommunity ev = communityEventRepository.findById(eventId).orElse(null);
        if (ev == null) return;

        if (ev.getStatus() == EventCommunity.EventStatus.CANCELLED) {
            throw new RegistrationClosedException(ev.getTitle(), "Event has been cancelled");
        }

        boolean isNewRegistration = (registration.getId() == null);
        if (isNewRegistration && userId != null) {
            if (repository.existsByUserIdAndActivityIdAndStatusNot(userId, "event-" + eventId, "CANCELLED")
                    || repository.existsByUserIdAndActivityIdAndStatusNot(userId, String.valueOf(eventId), "CANCELLED")) {
                throw new AlreadyRegisteredException(ev.getTitle(), "You are already registered for the event: '" + ev.getTitle() + "'.");
            }
        }

        if (ev.getRegistrationDeadline() != null && LocalDate.now().isAfter(ev.getRegistrationDeadline())) {
            throw new RegistrationClosedException(ev.getTitle(), "past deadline (" + ev.getRegistrationDeadline() + ")");
        }

        int requestedDevotees = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                ? registration.getDevoteeCount() : 1;

        // 1. Overall Event Capacity Check (check maxAttendees or capacity)
        Integer maxLimit = ev.getMaxAttendees() != null ? ev.getMaxAttendees() : ev.getCapacity();
        if (maxLimit != null && maxLimit > 0) {
            List<EventBookingRegistration> existingBookings = new java.util.ArrayList<>();
            existingBookings.addAll(repository.findByActivityId("event-" + eventId));
            existingBookings.addAll(repository.findByActivityId(String.valueOf(eventId)));

            long bookingAttendeeCount = existingBookings.stream()
                    .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                    .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                    .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                    .sum();

            long currentOccupancy = bookingAttendeeCount;
            if (currentOccupancy + requestedDevotees > maxLimit) {
                throw new EventFullException(ev.getTitle() + " (Capacity: " + maxLimit + ", Currently Registered: " + currentOccupancy + ")", maxLimit);
            }
        }

        // 2. Specific Ticket Category Capacity Check
        if (ticketCategoryRepository != null && registration.getCategory() != null && !registration.getCategory().isBlank()) {
            List<EventTicketCategory> ticketCategories = ticketCategoryRepository.findByEventIdOrderByDisplayOrderAscIdAsc(eventId);
            if (ticketCategories != null && !ticketCategories.isEmpty()) {
                String regCatName = registration.getCategory().trim().toLowerCase();
                for (EventTicketCategory cat : ticketCategories) {
                    if (cat.getName() != null && (cat.getName().trim().toLowerCase().equals(regCatName)
                            || (cat.getTicketCode() != null && cat.getTicketCode().equalsIgnoreCase(regCatName)))) {
                        Integer catCap = cat.getCapacity() != null ? cat.getCapacity() : cat.getSeats();
                        if (catCap != null && catCap > 0) {
                            List<EventBookingRegistration> existingBookings = new java.util.ArrayList<>();
                            existingBookings.addAll(repository.findByActivityId("event-" + eventId));
                            existingBookings.addAll(repository.findByActivityId(String.valueOf(eventId)));

                            long catAttendees = existingBookings.stream()
                                    .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                                    .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                                    .filter(b -> b.getCategory() != null && b.getCategory().trim().equalsIgnoreCase(cat.getName().trim()))
                                    .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                                    .sum();

                            if (catAttendees + requestedDevotees > catCap) {
                                throw new EventFullException("Pass Category '" + cat.getName() + "' is fully booked (Capacity: " + catCap + ")", catCap);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private int computeDevoteeCount(Integer currentCount, String attendingDevotees, String membersJson) {
        int best = (currentCount != null && currentCount > 0) ? currentCount : 1;

        // Parse membersJson (JSON array of member objects [{name,age,...}])
        if (membersJson != null && !membersJson.isBlank()) {
            String mj = membersJson.trim();
            if (mj.startsWith("[") && mj.endsWith("]")) {
                int count = 0;
                boolean inString = false;
                for (int i = 0; i < mj.length(); i++) {
                    char c = mj.charAt(i);
                    if (c == '"' && (i == 0 || mj.charAt(i - 1) != '\\')) inString = !inString;
                    else if (!inString && c == '{') count++;
                }
                if (count > best) best = count;
            }
        }

        // Parse attendingDevotees (comma-separated names or JSON array of strings)
        if (attendingDevotees != null && !attendingDevotees.isBlank()) {
            String trimmed = attendingDevotees.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                int count = 0;
                boolean inString = false;
                for (int i = 0; i < trimmed.length(); i++) {
                    char c = trimmed.charAt(i);
                    if (c == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                        inString = !inString;
                    } else if (!inString && c == '{') {
                        count++;
                    }
                }
                // If no objects found, count quoted strings (array of names)
                if (count == 0) {
                    String inner = trimmed.substring(1, trimmed.length() - 1).trim();
                    if (!inner.isEmpty()) {
                        String[] parts = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                        count = 0;
                        for (String p : parts) {
                            if (!p.trim().replace("\"", "").trim().isEmpty()) count++;
                        }
                    }
                }
                if (count > best) best = count;
            } else {
                String[] parts = trimmed.split(",");
                int validParts = 0;
                for (String p : parts) {
                    if (!p.trim().isEmpty()) validParts++;
                }
                if (validParts > best) best = validParts;
            }
        }

        return best;
    }

    private void decrementActivitySlots(EventBookingRegistration registration) {
        String actId = registration.getActivityId();
        if (actId == null || actId.isBlank()) return;

        int booked = (registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0)
                ? registration.getDevoteeCount()
                : 1;

        try {
            if (actId.startsWith("pooja-")) {
                // Capacity is owned by event_pooja_schedule.devotee_capacity, enforced atomically at
                // reserve time (SELECT FOR UPDATE). event_pooja_sevas.slots is a config value only.
                return;
            } else if (actId.startsWith("food-")) {
                // target_plates is a fixed total ceiling; capacity is checked live from booking rows.
                // Do NOT mutate target_plates — doing so would corrupt the ceiling used in validation.
            } else if (actId.startsWith("comp-")) {
                Long id = Long.parseLong(actId.replace("comp-", ""));
                competitionRepository.findById(id).ifPresent(c -> {
                    int current = c.getMaxParticipants() != null ? c.getMaxParticipants() : 50;
                    c.setMaxParticipants(Math.max(0, current - booked));
                    competitionRepository.save(c);
                });
            } else if (actId.startsWith("event-")) {
                Long id = Long.parseLong(actId.replace("event-", ""));
                communityEventRepository.findById(id).ifPresent(ev -> {
                    if (ev.getCapacity() != null) {
                        ev.setCapacity(Math.max(0, ev.getCapacity() - booked));
                    }
                    // maxAttendees is the fixed total maximum — do not decrement it
                    communityEventRepository.save(ev);
                });
            } else {
                try {
                    Long id = Long.parseLong(actId);
                    communityEventRepository.findById(id).ifPresent(ev -> {
                        if (ev.getCapacity() != null) {
                            ev.setCapacity(Math.max(0, ev.getCapacity() - booked));
                        }
                        // maxAttendees is the fixed total maximum — do not decrement it
                        communityEventRepository.save(ev);
                    });
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ex) {
            // Non-critical slot decrement failure
        }
    }

    private void incrementActivitySlots(EventBookingRegistration registration) {
        String actId = registration.getActivityId();
        if (actId == null || actId.isBlank()) return;

        int booked = (registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0)
                ? registration.getDevoteeCount()
                : 1;

        try {
            if (actId.startsWith("pooja-")) {
                // Reservation release is handled in syncToPoojaTableIfApplicable when status = CANCELLED.
                return;
            } else if (actId.startsWith("food-")) {
                // target_plates is a fixed total ceiling — do not restore it on cancellation.
            } else if (actId.startsWith("comp-")) {
                Long id = Long.parseLong(actId.replace("comp-", ""));
                competitionRepository.findById(id).ifPresent(c -> {
                    int current = c.getMaxParticipants() != null ? c.getMaxParticipants() : 50;
                    c.setMaxParticipants(current + booked);
                    competitionRepository.save(c);
                });
            } else if (actId.startsWith("event-")) {
                Long id = Long.parseLong(actId.replace("event-", ""));
                communityEventRepository.findById(id).ifPresent(ev -> {
                    if (ev.getCapacity() != null) {
                        ev.setCapacity(ev.getCapacity() + booked);
                    }
                    communityEventRepository.save(ev);
                });
            } else {
                try {
                    Long id = Long.parseLong(actId);
                    communityEventRepository.findById(id).ifPresent(ev -> {
                        if (ev.getCapacity() != null) {
                            ev.setCapacity(ev.getCapacity() + booked);
                        }
                        communityEventRepository.save(ev);
                    });
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ex) {
            // Non-critical slot increment failure
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventBookingRegistration> getMyRegistrations(AppUser user, Long communityId) {
        return getMyRegistrations(user, communityId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventBookingRegistration> getMyRegistrations(AppUser user, Long communityId, String status) {
        if (user == null || user.getId() == null) {
            if (communityId != null) {
                if (status != null && !status.isBlank()) {
                    if ("ACTIVE".equalsIgnoreCase(status.trim())) {
                        return repository.findByCommunityIdAndStatusNotOrderByCreatedAtDesc(communityId, "CANCELLED");
                    }
                    return repository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status.trim().toUpperCase());
                }
                return repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
            }
            return Collections.emptyList();
        }

        List<EventBookingRegistration> list = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) {
            if ("ACTIVE".equalsIgnoreCase(status.trim())) {
                list.addAll(repository.findByUserIdAndStatusNotOrderByCreatedAtDesc(user.getId(), "CANCELLED"));
            } else {
                list.addAll(repository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status.trim().toUpperCase()));
            }
        } else {
            list.addAll(repository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        }

        // Include any meal registrations from event_meal_registrations not already in list
        try {
            List<EventMealRegistration> mealRegs = mealRegistrationRepository.findByUserId(user.getId());
            if (mealRegs != null && !mealRegs.isEmpty()) {
                java.util.Set<String> existingActIds = list.stream()
                        .map(EventBookingRegistration::getActivityId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());

                for (EventMealRegistration mr : mealRegs) {
                    Long ldId = mr.getLunchDinner() != null ? mr.getLunchDinner().getId() : mr.getId();
                    String actId = "meal-" + ldId;
                    String foodActId = "food-" + ldId;
                    if (!existingActIds.contains(actId) && !existingActIds.contains(foodActId)) {
                        EventBookingRegistration synth = new EventBookingRegistration();
                        synth.setId(mr.getId());
                        synth.setUser(user);
                        synth.setParticipantName(user.getFullName() != null ? user.getFullName() : user.getUsername());
                        synth.setPhone(user.getPhone());
                        synth.setCategory("Food");
                        synth.setPassType("Meal Registration Pass");
                        synth.setActivityType("LUNCH_DINNER");
                        synth.setActivityId(actId);
                        synth.setMainEventId(mr.getEvent() != null ? mr.getEvent().getId() : null);
                        synth.setEventId(mr.getEvent() != null ? mr.getEvent().getId() : null);
                        String title = mr.getLunchDinner() != null && mr.getLunchDinner().getName() != null
                                ? mr.getLunchDinner().getName()
                                : (mr.getMealType() != null ? mr.getMealType().name() + " Feast" : "Community Feast");
                        synth.setActivityTitle(title);
                        synth.setEventName(mr.getEvent() != null ? mr.getEvent().getTitle() : title);
                        synth.setEventDate(mr.getMealDate() != null ? mr.getMealDate().toString() : null);
                        if (mr.getLunchDinner() != null && mr.getLunchDinner().getStartTime() != null) {
                            String t = mr.getLunchDinner().getStartTime().toString();
                            if (mr.getLunchDinner().getEndTime() != null) {
                                t += " - " + mr.getLunchDinner().getEndTime().toString();
                            }
                            synth.setEventTime(t);
                        }
                        if (mr.getLunchDinner() != null && mr.getLunchDinner().getVenue() != null) {
                            synth.setVenue(mr.getLunchDinner().getVenue());
                        }
                        synth.setDevoteeCount(mr.getHeadCount() != null ? mr.getHeadCount() : 1);
                        synth.setStatus("CONFIRMED");
                        synth.setPaymentStatus("FREE");
                        synth.setBookingFee(0.0);
                        synth.setRegCode("MNA-2026-MEAL-" + String.format("%06d", mr.getId()));
                        synth.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + synth.getRegCode());
                        synth.setCreatedAt(mr.getCreatedAt() != null ? mr.getCreatedAt() : LocalDateTime.now());
                        list.add(synth);
                    }
                }
            }
        } catch (Exception ignored) {}

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventBookingRegistration> getRegistrationsByCommunity(Long communityId) {
        return getRegistrationsByCommunity(communityId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventBookingRegistration> getRegistrationsByCommunity(Long communityId, String status) {
        if (communityId != null) {
            if (status != null && !status.isBlank()) {
                if ("ACTIVE".equalsIgnoreCase(status.trim())) {
                    return repository.findByCommunityIdAndStatusNotOrderByCreatedAtDesc(communityId, "CANCELLED");
                }
                return repository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status.trim().toUpperCase());
            }
            return repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public EventBookingRegistration getRegistrationById(Long id, AppUser user) {
        boolean isAdmin = isUserAdmin(user);
        if (isAdmin) {
            return repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + id));
        }
        if (user != null && user.getId() != null) {
            return repository.findByIdAndUserId(id, user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + id));
        }
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + id));
    }

    @Override
    @Transactional
    public void cancelRegistration(Long id, AppUser user) {
        cancelRegistration(id, "Cancelled by user", user);
    }

    @Override
    @Transactional
    public void cancelRegistration(Long id, String reason, AppUser user) {
        EventBookingRegistration reg = getRegistrationById(id, user);
        if (!"CANCELLED".equalsIgnoreCase(reg.getStatus())) {
            reg.setStatus("CANCELLED");
            reg.setCancellationReason(reason != null && !reason.isBlank() ? reason.trim() : "Cancelled");
            reg.setCancelledAt(LocalDateTime.now());
            reg.setUpdatedAt(LocalDateTime.now());
            EventBookingRegistration saved = repository.save(reg);

            // Restore slot capacity
            incrementActivitySlots(saved);

            syncToPoojaTableIfApplicable(saved);
        }
    }

    @Override
    @Transactional
    public void deleteRegistration(Long id, AppUser user) {
        EventBookingRegistration reg = getRegistrationById(id, user);
        // Restore slot capacity before deleting
        if (!"CANCELLED".equalsIgnoreCase(reg.getStatus())) {
            incrementActivitySlots(reg);
        }
        if (reg.getRegCode() != null && poojaUserRegRepo != null) {
            poojaUserRegRepo.findByRegCode(reg.getRegCode()).ifPresent(poojaUserRegRepo::delete);
        }
        repository.delete(reg);
    }

    @Override
    @Transactional
    public EventBookingRegistration updateRegistration(Long id, EventBookingRegistration patch, AppUser user) {
        EventBookingRegistration reg = getRegistrationById(id, user);
        boolean isAdmin = isUserAdmin(user);

        // Only block user if already cancelled, unless admin is updating/reactivating
        if (!isAdmin && "CANCELLED".equalsIgnoreCase(reg.getStatus())) {
            throw new IllegalStateException("Cannot update a cancelled registration");
        }

        // ── Admin-controlled status, fee & category fields ───────────────────
        if (isAdmin && patch.getStatus() != null && !patch.getStatus().isBlank()) {
            reg.setStatus(patch.getStatus().trim().toUpperCase());
        }
        if (isAdmin && patch.getPaymentStatus() != null && !patch.getPaymentStatus().isBlank()) {
            reg.setPaymentStatus(patch.getPaymentStatus().trim().toUpperCase());
        }
        if (isAdmin && patch.getBookingFee() != null) {
            reg.setBookingFee(patch.getBookingFee());
        }
        if (isAdmin && patch.getCategory() != null && !patch.getCategory().isBlank()) {
            reg.setCategory(patch.getCategory().trim());
        }
        if (isAdmin && patch.getActivityTitle() != null && !patch.getActivityTitle().isBlank()) {
            reg.setActivityTitle(patch.getActivityTitle().trim());
        }

        // ── Mutable identity / contact fields ────────────────────────────────
        if (patch.getParticipantName() != null && !patch.getParticipantName().isBlank()) {
            reg.setParticipantName(patch.getParticipantName().trim());
        }
        if (patch.getGotram() != null) {
            reg.setGotram(patch.getGotram().trim());
        }
        if (patch.getAttendingDevotees() != null) {
            reg.setAttendingDevotees(patch.getAttendingDevotees());
        }
        if (patch.getMembersJson() != null) {
            reg.setMembersJson(patch.getMembersJson());
        }
        if (patch.getDevoteeCount() != null && patch.getDevoteeCount() > 0) {
            reg.setDevoteeCount(patch.getDevoteeCount());
        }
        // Always recompute devoteeCount from all available sources to get the max
        reg.setDevoteeCount(computeDevoteeCount(reg.getDevoteeCount(), reg.getAttendingDevotees(), reg.getMembersJson()));

        // ── Slot change: new day / time selection ────────────────────────────
        // eventDate and eventTime are the canonical slot fields.
        // When a user picks a different day or slot we MUST update them so the
        // slot-decrement, QR code, and duplicate-check logic all see the correct slot.
        if (patch.getEventDate() != null && !patch.getEventDate().isBlank()) {
            reg.setEventDate(patch.getEventDate().trim());
        }
        if (patch.getEventTime() != null && !patch.getEventTime().isBlank()) {
            reg.setEventTime(patch.getEventTime().trim());
        }
        // Venue may change if the updated slot is in a different mandap
        if (patch.getVenue() != null && !patch.getVenue().isBlank()) {
            reg.setVenue(patch.getVenue().trim());
        }
        // Pass type (e.g. Pooja Registration Pass)
        if (patch.getPassType() != null && !patch.getPassType().isBlank()) {
            reg.setPassType(patch.getPassType().trim());
        }

        // ── Payment fields ───────────────────────────────────────────────────
        if (patch.getPaymentReceiptUrl() != null) {
            reg.setPaymentReceiptUrl(patch.getPaymentReceiptUrl());
        }
        if (patch.getTransactionId() != null) {
            reg.setTransactionId(patch.getTransactionId());
        }
        if (patch.getPaymentMethod() != null) {
            reg.setPaymentMethod(patch.getPaymentMethod());
        }

        if (patch.getMainEventId() != null) {
            reg.setMainEventId(patch.getMainEventId());
        } else if (reg.getMainEventId() == null) {
            reg.setMainEventId(resolveMainEventId(reg.getActivityId()));
        }

        reg.setUpdatedAt(LocalDateTime.now());
        EventBookingRegistration saved = repository.save(reg);

        syncToPoojaTableIfApplicable(saved);

        return saved;
    }

    private Long resolveMainEventId(String actId) {
        if (actId == null || actId.isBlank()) return null;
        try {
            if (actId.startsWith("event-")) {
                return Long.parseLong(actId.replace("event-", ""));
            } else if (actId.startsWith("pooja-")) {
                Long poojaId = Long.parseLong(actId.replace("pooja-", ""));
                return poojaSevaRepository.findById(poojaId)
                        .map(EventPoojaSeva::getMainEventId)
                        .orElse(null);
            } else if (actId.startsWith("food-")) {
                Long foodId = Long.parseLong(actId.replace("food-", ""));
                return lunchDinnerRepository.findById(foodId)
                        .map(EventLunchDinner::getMainEventId)
                        .orElse(null);
            } else if (actId.startsWith("comp-")) {
                Long compId = Long.parseLong(actId.replace("comp-", ""));
                return competitionRepository.findById(compId)
                        .map(EventCompetition::getMainEventId)
                        .orElse(null);
            } else if (actId.startsWith("cultural-") || actId.startsWith("cult-")) {
                Long cultId = Long.parseLong(actId.replaceAll("^(cultural|cult)-", ""));
                return culturalEventRepository.findById(cultId)
                        .map(EventCulturalEvent::getMainEventId)
                        .orElse(null);
            } else {
                try {
                    return Long.parseLong(actId);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private void syncToPoojaTableIfApplicable(EventBookingRegistration reg) {
        if (reg == null || poojaUserRegRepo == null) return;
        boolean isPooja = (reg.getCategory() != null && reg.getCategory().equalsIgnoreCase("Pooja")) ||
                (reg.getActivityId() != null && reg.getActivityId().startsWith("pooja-"));
        if (!isPooja) return;

        try {
            Long eventId = reg.getMainEventId();
            if (eventId == null && reg.getActivityId() != null && reg.getActivityId().startsWith("pooja-")) {
                try {
                    eventId = Long.parseLong(reg.getActivityId().replace("pooja-", ""));
                } catch (Exception ignored) {}
            }

            EventPoojaUserRegistration poojaReg = poojaUserRegRepo.findByRegCode(reg.getRegCode())
                    .orElse(EventPoojaUserRegistration.builder().regCode(reg.getRegCode()).build());

            String userPhone = (reg.getUser() != null) ? reg.getUser().getPhone() : null;
            String userEmail = (reg.getUser() != null) ? reg.getUser().getEmail() : null;

            poojaReg.setEventId(eventId);
            poojaReg.setUser(reg.getUser());
            poojaReg.setCommunity(reg.getCommunity());
            poojaReg.setParticipantName(reg.getParticipantName());
            poojaReg.setGotram(reg.getGotram());
            poojaReg.setPhone(userPhone);
            poojaReg.setEmail(userEmail);
            poojaReg.setDevoteeCount(reg.getDevoteeCount());
            poojaReg.setAttendingDevotees(reg.getAttendingDevotees());
            poojaReg.setPoojaSlotName(reg.getActivityTitle());
            poojaReg.setPoojaSlotDate(reg.getEventDate());
            poojaReg.setPoojaSlotTime(reg.getEventTime());
            poojaReg.setVenue(reg.getVenue());
            poojaReg.setCategory("Pooja");
            poojaReg.setBookingFee(reg.getBookingFee());
            poojaReg.setPaymentStatus(reg.getPaymentStatus());
            poojaReg.setPaymentMethod(reg.getPaymentMethod());
            poojaReg.setTransactionId(reg.getTransactionId());
            poojaReg.setPaymentReceiptUrl(reg.getPaymentReceiptUrl());
            poojaReg.setStatus(reg.getStatus());
            poojaReg.setQrCodeUrl(reg.getQrCodeUrl());

            // Stamp reservation fields on first save or update (reservation_id is @Transient on EventBookingRegistration)
            Long incomingReservationId = reg.getReservationId();
            if (incomingReservationId != null) {
                poojaSlotReservationRepository.findById(incomingReservationId).ifPresent(reservation -> {
                    poojaReg.setReservationId(reservation.getId());
                    if (reservation.getTokenNumber() != null) {
                        poojaReg.setTokenNumber(reservation.getTokenNumber());
                    }
                    if (reservation.getSchedule() != null) {
                        poojaReg.setScheduleId(reservation.getSchedule().getId());
                        if (reservation.getSchedule().getTimeSlotConfigId() != null) {
                            poojaReg.setPoojaSevaTimeSlotsId(reservation.getSchedule().getTimeSlotConfigId());
                        }
                    }
                });
            }

            // Auto-resolve correct scheduleId from schedule table if not set or if date/time changed
            if (poojaReg.getPoojaSlotDate() != null && poojaReg.getPoojaSlotTime() != null) {
                try {
                    Long poojaId = poojaReg.getPoojaSevaId() != null ? poojaReg.getPoojaSevaId() : poojaReg.getEventId();
                    if (poojaId != null) {
                        java.time.LocalDate d = java.time.LocalDate.parse(poojaReg.getPoojaSlotDate().trim());
                        String tStr = poojaReg.getPoojaSlotTime().trim();
                        if (tStr.contains(" - ")) tStr = tStr.split(" - ")[0].trim();
                        if (tStr.length() == 5) tStr += ":00";
                        java.time.LocalTime t = java.time.LocalTime.parse(tStr);
                        scheduleRepository.findByPoojaSeva_IdAndScheduleDateAndStartTime(poojaId, d, t)
                                .ifPresent(sch -> {
                                    poojaReg.setScheduleId(sch.getId());
                                    if (sch.getTimeSlotConfigId() != null) {
                                        poojaReg.setPoojaSevaTimeSlotsId(sch.getTimeSlotConfigId());
                                    }
                                });
                    }
                } catch (Exception ignored) {}
            }

            EventPoojaUserRegistration savedPoojaReg = poojaUserRegRepo.save(poojaReg);

            // Confirm or release the linked reservation based on final registration status
            Long linkedReservationId = savedPoojaReg.getReservationId() != null
                    ? savedPoojaReg.getReservationId()
                    : incomingReservationId;
            if (linkedReservationId != null) {
                if ("CANCELLED".equalsIgnoreCase(reg.getStatus())) {
                    poojaSlotReservationService.releaseReservation(linkedReservationId);
                } else {
                    poojaSlotReservationService.confirmReservation(linkedReservationId, savedPoojaReg.getId());
                }
            }
        } catch (Exception ignored) {}
    }
}
