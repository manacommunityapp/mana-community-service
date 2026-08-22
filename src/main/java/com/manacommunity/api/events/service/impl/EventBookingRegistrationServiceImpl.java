package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.Competition;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.LunchDinner;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.entity.PoojaSevaDayTimeSlot;
import com.manacommunity.api.events.entity.EventTicketCategory;
import com.manacommunity.api.events.entity.CulturalEvent;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.repository.EventTicketCategoryRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.service.EventBookingRegistrationService;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private final CommunityEventRepository communityEventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventTicketCategoryRepository ticketCategoryRepository;
    private final AppUserRepository appUserRepository;

    public EventBookingRegistrationServiceImpl(
            EventBookingRegistrationRepository repository,
            EventPoojaUserRegistrationRepository poojaUserRegRepo,
            CommunityRepository communityRepository,
            PoojaSevaRepository poojaSevaRepository,
            LunchDinnerRepository lunchDinnerRepository,
            CompetitionRepository competitionRepository,
            CulturalEventRepository culturalEventRepository,
            CommunityEventRepository communityEventRepository,
            EventRegistrationRepository eventRegistrationRepository,
            EventTicketCategoryRepository ticketCategoryRepository,
            AppUserRepository appUserRepository) {
        this.repository = repository;
        this.poojaUserRegRepo = poojaUserRegRepo;
        this.communityRepository = communityRepository;
        this.poojaSevaRepository = poojaSevaRepository;
        this.lunchDinnerRepository = lunchDinnerRepository;
        this.competitionRepository = competitionRepository;
        this.culturalEventRepository = culturalEventRepository;
        this.communityEventRepository = communityEventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.appUserRepository = appUserRepository;
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
            validateCapacityAndDeadline(registration, validatingUserId);
        }

        Community comm = (targetUser != null && targetUser.getCommunity() != null)
                ? targetUser.getCommunity()
                : (user != null && user.getCommunity() != null)
                        ? user.getCommunity()
                        : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

        registration.setId(null);
        registration.setUser(targetUser);
        registration.setCommunity(comm);

        if (registration.getRegCode() == null || registration.getRegCode().isBlank()) {
            String prefix = "MNA-2026-REG-";
            if (registration.getCategory() != null) {
                String catCode = registration.getCategory().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
                if (catCode.length() > 4) catCode = catCode.substring(0, 4);
                if (!catCode.isEmpty()) prefix = "MNA-2026-" + catCode + "-";
            }
            int rnd = new Random().nextInt(9000) + 1000;
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
                validateLunchDinnerCapacityAndDeadline(id, registration);
            } else if (actId.startsWith("comp-")) {
                Long id = Long.parseLong(actId.replace("comp-", ""));
                validateCompetitionCapacityAndDeadline(id, registration);
            } else if (actId.startsWith("cultural-") || actId.startsWith("cult-")) {
                Long id = Long.parseLong(actId.replaceAll("^(cultural|cult)-", ""));
                validateCulturalCapacityAndDeadline(id, registration);
            } else if (actId.startsWith("event-")) {
                Long id = Long.parseLong(actId.replace("event-", ""));
                validateEventCapacityAndDeadline(id, registration);
            } else {
                try {
                    Long id = Long.parseLong(actId);
                    validateEventCapacityAndDeadline(id, registration);
                } catch (NumberFormatException ignored) {}
            }
        } catch (AlreadyRegisteredException | EventFullException | RegistrationClosedException ex) {
            throw ex;
        } catch (Exception ignored) {}
    }

    private void validatePoojaCapacityAndDeadline(Long id, EventBookingRegistration registration, Long userId) {
        PoojaSeva p = poojaSevaRepository.findById(id).orElse(null);
        if (p == null) return;

        // Check if parent event is cancelled
        if (p.getMainEventId() != null) {
            CommunityEvent parent = communityEventRepository.findById(p.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == CommunityEvent.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(p.getName(), "Parent event has been cancelled");
            }
        }

        // Only check duplicate for new registrations (not updates)
        boolean isNewRegistration = (registration.getId() == null);

        // 1. Block re-registration for the exact same seva
        if (isNewRegistration && userId != null
                && repository.existsByUserIdAndActivityIdAndStatusNot(userId, "pooja-" + id, "CANCELLED")) {
            throw new AlreadyRegisteredException(p.getName());
        }

        // 2. Block registration if user already has an active booking for ANY seva in the same parent event
        if (isNewRegistration && userId != null && p.getMainEventId() != null) {
            List<PoojaSeva> siblingSevas = poojaSevaRepository
                    .findByMainEventIdOrderByDateAscStartTimeAsc(p.getMainEventId());
            List<String> siblingActivityIds = siblingSevas.stream()
                    .map(s -> "pooja-" + s.getId())
                    .collect(java.util.stream.Collectors.toList());
            if (!siblingActivityIds.isEmpty()
                    && repository.existsByUserIdAndActivityIdInAndStatusNot(userId, siblingActivityIds, "CANCELLED")) {
                throw new AlreadyRegisteredException(
                        p.getName() + "' Event — only one pooja seva registration is allowed per family per event");
            }
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

    private void validateCulturalCapacityAndDeadline(Long id, EventBookingRegistration registration) {
        if (culturalEventRepository == null) return;
        CulturalEvent c = culturalEventRepository.findById(id).orElse(null);
        if (c == null) return;

        // Check if parent event is cancelled
        if (c.getMainEventId() != null) {
            CommunityEvent parent = communityEventRepository.findById(c.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == CommunityEvent.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(c.getName(), "Parent event has been cancelled");
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

    private void validateCompetitionCapacityAndDeadline(Long id, EventBookingRegistration registration) {
        Competition c = competitionRepository.findById(id).orElse(null);
        if (c == null) return;

        // Check if parent event is cancelled
        if (c.getMainEventId() != null) {
            CommunityEvent parent = communityEventRepository.findById(c.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == CommunityEvent.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(c.getName(), "Parent event has been cancelled");
            }
        }

        LocalDate today = LocalDate.now();
        if (c.getRegDeadline() != null && today.isAfter(c.getRegDeadline())) {
            throw new RegistrationClosedException(c.getName(), "Registration deadline has passed (" + c.getRegDeadline() + ")");
        }
        if (c.getDate() != null && c.getDate().isBefore(today)) {
            throw new RegistrationClosedException(c.getName(), "Competition date has passed (" + c.getDate() + ")");
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

    private void validateLunchDinnerCapacityAndDeadline(Long id, EventBookingRegistration registration) {
        LunchDinner m = lunchDinnerRepository.findById(id).orElse(null);
        if (m == null) return;

        // Check if parent event is cancelled
        if (m.getMainEventId() != null) {
            CommunityEvent parent = communityEventRepository.findById(m.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == CommunityEvent.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(m.getName(), "Parent event has been cancelled");
            }
        }

        LocalDate today = LocalDate.now();
        if (m.getDate() != null && m.getDate().isBefore(today)) {
            throw new RegistrationClosedException(m.getName(), "Meal date has passed (" + m.getDate() + ")");
        }

        int maxPlates = m.getTargetPlates() != null && m.getTargetPlates() > 0 ? m.getTargetPlates() : 500;
        int requested = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                ? registration.getDevoteeCount() : 1;

        List<EventBookingRegistration> existing = repository.findByActivityId("food-" + id);
        long bookedPlates = existing.stream()
                .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                .sum();

        if (bookedPlates + requested > maxPlates) {
            throw new EventFullException(m.getName() + " (Target Plates: " + maxPlates + ", Currently Booked: " + bookedPlates + ")", maxPlates);
        }
    }

    private void validateEventCapacityAndDeadline(Long eventId, EventBookingRegistration registration) {
        CommunityEvent ev = communityEventRepository.findById(eventId).orElse(null);
        if (ev == null) return;

        if (ev.getStatus() == CommunityEvent.EventStatus.CANCELLED) {
            throw new RegistrationClosedException(ev.getTitle(), "Event has been cancelled");
        }

        if (ev.getRegistrationDeadline() != null && LocalDate.now().isAfter(ev.getRegistrationDeadline())) {
            throw new RegistrationClosedException(ev.getTitle(), "past deadline (" + ev.getRegistrationDeadline() + ")");
        }

        int requestedDevotees = registration.getDevoteeCount() != null && registration.getDevoteeCount() > 0
                ? registration.getDevoteeCount() : 1;

        // 1. Overall Event Capacity Check (check maxAttendees or capacity)
        Integer maxLimit = ev.getMaxAttendees() != null ? ev.getMaxAttendees() : ev.getCapacity();
        if (maxLimit != null && maxLimit > 0) {
            long directCount = eventRegistrationRepository != null ? eventRegistrationRepository.countByEventId(eventId) : 0;
            List<EventBookingRegistration> existingBookings = new java.util.ArrayList<>();
            existingBookings.addAll(repository.findByActivityId("event-" + eventId));
            existingBookings.addAll(repository.findByActivityId(String.valueOf(eventId)));

            long bookingAttendeeCount = existingBookings.stream()
                    .filter(b -> (registration.getId() == null || !registration.getId().equals(b.getId())))
                    .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                    .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                    .sum();

            long currentOccupancy = Math.max(directCount, bookingAttendeeCount);
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
                Long id = Long.parseLong(actId.replace("pooja-", ""));
                poojaSevaRepository.findById(id).ifPresent(p -> {
                    if (!decrementPoojaTimeSlot(p, registration, booked)) {
                        int current = p.getSlots() != null ? p.getSlots() : 20;
                        p.setSlots(Math.max(0, current - booked));
                    }
                    poojaSevaRepository.save(p);
                });
            } else if (actId.startsWith("food-")) {
                Long id = Long.parseLong(actId.replace("food-", ""));
                lunchDinnerRepository.findById(id).ifPresent(m -> {
                    int current = m.getTargetPlates() != null ? m.getTargetPlates() : 500;
                    m.setTargetPlates(Math.max(0, current - booked));
                    lunchDinnerRepository.save(m);
                });
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

    private boolean decrementPoojaTimeSlot(PoojaSeva poojaSeva, EventBookingRegistration registration, int booked) {
        if (poojaSeva == null || !Boolean.TRUE.equals(poojaSeva.getMultiDay())) return false;
        if (poojaSeva.getTimeSlotConfig() == null || poojaSeva.getTimeSlotConfig().isEmpty()) return false;

        LocalDate bookedDate = parseBookingDate(registration.getEventDate());
        LocalTime bookedTime = parseBookingTime(registration.getEventTime());
        if (bookedDate == null || bookedTime == null) return false;

        for (PoojaSevaDayTimeSlot slot : poojaSeva.getTimeSlotConfig()) {
            if (slot == null || slot.getSlotDate() == null || slot.getStartTime() == null) continue;
            LocalTime slotTime = parseBookingTime(slot.getStartTime());
            if (bookedDate.equals(slot.getSlotDate()) && bookedTime.equals(slotTime)) {
                int current = slot.getSlotCount() != null ? slot.getSlotCount() : defaultPoojaSlotCount(poojaSeva);
                slot.setSlotCount(Math.max(0, current - booked));
                return true;
            }
        }

        return false;
    }

    private int defaultPoojaSlotCount(PoojaSeva poojaSeva) {
        return poojaSeva.getSlots() != null ? poojaSeva.getSlots() : 20;
    }

    private LocalDate parseBookingDate(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.contains("T")) clean = clean.substring(0, clean.indexOf('T'));
        try {
            return LocalDate.parse(clean);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalTime parseBookingTime(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.contains("T")) clean = clean.substring(clean.indexOf('T') + 1);
        if (clean.length() >= 8 && clean.charAt(2) == ':' && clean.charAt(5) == ':') {
            clean = clean.substring(0, 8);
        }

        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ISO_LOCAL_TIME,
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        );
        for (DateTimeFormatter format : formats) {
            try {
                return LocalTime.parse(clean.toUpperCase(Locale.ENGLISH), format);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventBookingRegistration> getMyRegistrations(AppUser user, Long communityId) {
        if (user == null || user.getId() == null) {
            if (communityId != null) {
                return repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
            }
            return Collections.emptyList();
        }

        return repository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventBookingRegistration> getRegistrationsByCommunity(Long communityId) {
        if (communityId != null) {
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
        EventBookingRegistration reg = getRegistrationById(id, user);
        reg.setStatus("CANCELLED");
        reg.setUpdatedAt(LocalDateTime.now());
        EventBookingRegistration saved = repository.save(reg);
        syncToPoojaTableIfApplicable(saved);
    }

    @Override
    @Transactional
    public void deleteRegistration(Long id, AppUser user) {
        EventBookingRegistration reg = getRegistrationById(id, user);
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

        // ── Parent event link — persist if the frontend sends it ─────────────
        if (patch.getMainEventId() != null) {
            reg.setMainEventId(patch.getMainEventId());
        }

        reg.setUpdatedAt(LocalDateTime.now());
        EventBookingRegistration saved = repository.save(reg);

        syncToPoojaTableIfApplicable(saved);

        return saved;
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

            poojaUserRegRepo.save(poojaReg);
        } catch (Exception ignored) {}
    }
}
