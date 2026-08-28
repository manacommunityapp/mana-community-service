package com.manacommunity.api.events.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.dto.PoojaReserveRequest;
import com.manacommunity.api.events.dto.PoojaReserveResponse;
import com.manacommunity.api.events.entity.EventPoojaBookingParticipant;
import com.manacommunity.api.events.entity.EventPoojaSchedule;
import com.manacommunity.api.events.entity.EventPoojaSlotReservation;
import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.events.enums.RegistrationSource;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventPoojaBookingParticipantRepository;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.repository.EventPoojaScheduleRepository;
import com.manacommunity.api.events.repository.EventPoojaSlotReservationRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.service.EventPoojaUserRegistrationService;
import com.manacommunity.api.events.enums.PoojaRegistrationStatus;
import com.manacommunity.api.events.service.PoojaSlotReservationService;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class EventPoojaUserRegistrationServiceImpl implements EventPoojaUserRegistrationService {

    private final EventPoojaUserRegistrationRepository repository;
    private final CommunityRepository communityRepository;
    private final PoojaSlotReservationService reservationService;
    private final EventPoojaScheduleRepository scheduleRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventBookingRegistrationRepository eventBookingRegistrationRepository;
    private final EventPoojaSlotReservationRepository slotReservationRepository;
    private final EventPoojaBookingParticipantRepository participantRepository;
    private final ObjectMapper objectMapper;

    public EventPoojaUserRegistrationServiceImpl(
            EventPoojaUserRegistrationRepository repository,
            CommunityRepository communityRepository,
            PoojaSlotReservationService reservationService,
            EventPoojaScheduleRepository scheduleRepository,
            EventRegistrationRepository eventRegistrationRepository,
            EventBookingRegistrationRepository eventBookingRegistrationRepository,
            EventPoojaSlotReservationRepository slotReservationRepository,
            EventPoojaBookingParticipantRepository participantRepository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.communityRepository = communityRepository;
        this.reservationService = reservationService;
        this.scheduleRepository = scheduleRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.eventBookingRegistrationRepository = eventBookingRegistrationRepository;
        this.slotReservationRepository = slotReservationRepository;
        this.participantRepository = participantRepository;
        this.objectMapper = objectMapper;
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

    private boolean isRegisteredForMainEvent(Long mainEventId, Long userId) {
        if (mainEventId == null || mainEventId <= 0 || userId == null) {
            return true;
        }
        boolean isDirectRegistered = eventRegistrationRepository != null &&
                eventRegistrationRepository.existsByEventIdAndUserId(mainEventId, userId);
        if (isDirectRegistered) {
            return true;
        }
        if (eventBookingRegistrationRepository != null) {
            return eventBookingRegistrationRepository
                    .existsByUserIdAndActivityIdAndStatusNot(userId, "event-" + mainEventId, "CANCELLED")
                    || eventBookingRegistrationRepository
                    .existsByUserIdAndActivityIdAndStatusNot(userId, String.valueOf(mainEventId), "CANCELLED");
        }
        return false;
    }

    @Override
    @Transactional
    public EventPoojaUserRegistration createRegistration(EventPoojaUserRegistration registration, AppUser user, Long communityId) {
        return createRegistration(registration, user, communityId, false);
    }

    @Override
    @Transactional
    public EventPoojaUserRegistration createRegistration(EventPoojaUserRegistration registration, AppUser user, Long communityId, boolean adminOverride) {
        if (user != null) {
            registration.setUser(user);
        }

        if (communityId != null) {
            communityRepository.findById(communityId).ifPresent(registration::setCommunity);
        }

        // Enforce mandatory parent main event registration check if pooja belongs to a main event
        if (!adminOverride && user != null) {
            Long targetMainEventId = registration.getEventId();
            if ((targetMainEventId == null || targetMainEventId == 0) && registration.getScheduleId() != null) {
                targetMainEventId = scheduleRepository.findById(registration.getScheduleId())
                        .map(s -> s.getPoojaSeva() != null ? s.getPoojaSeva().getMainEventId() : null)
                        .orElse(null);
            }
            if (targetMainEventId != null && targetMainEventId > 0) {
                boolean isMainRegistered = isRegisteredForMainEvent(targetMainEventId, user.getId());
                if (!isMainRegistered) {
                    throw new IllegalArgumentException("Registration for the main event is required before booking this Pooja Seva. Please register for the main event first.");
                }
            }
        }

        // #1: Duplicate registration guard — skipped when adminOverride=true (#4)
        if (!adminOverride
                && user != null
                && registration.getEventId() != null
                && registration.getPoojaSlotDate() != null && !registration.getPoojaSlotDate().isBlank()) {
            boolean duplicate = repository.existsByUserIdAndEventIdAndPoojaSlotDateAndStatusNot(
                    user.getId(), registration.getEventId(), registration.getPoojaSlotDate(), "CANCELLED");
            if (duplicate) {
                String slotName = registration.getPoojaSlotName() != null ? registration.getPoojaSlotName() : "this pooja slot";
                throw new AlreadyRegisteredException(slotName,
                        "You already have an active registration for this pooja slot. " +
                        "Please cancel your existing registration before registering again.");
            }
        }

        // G-4: Schedule-level duplicate guard — catches cases where scheduleId is set
        // (new booking-engine path) independently of the string-based poojaSlotDate check.
        if (!adminOverride && user != null && registration.getScheduleId() != null) {
            boolean scheduleDuplicate = repository.existsByUserIdAndScheduleIdAndStatusNot(
                    user.getId(), registration.getScheduleId(), "CANCELLED");
            if (scheduleDuplicate) {
                String slotName = registration.getPoojaSlotName() != null ? registration.getPoojaSlotName() : "this pooja slot";
                throw new AlreadyRegisteredException(slotName,
                        "You already have an active registration for this Pooja slot. " +
                        "Please cancel your existing registration before booking again.");
            }
        }

        if (registration.getRegCode() == null || registration.getRegCode().isBlank()) {
            int year = java.time.LocalDate.now().getYear();
            String code;
            int attempts = 0;
            do {
                code = "MNA-" + year + "-POOJ-" + (1000 + new Random().nextInt(9000));
                attempts++;
            } while (repository.existsByRegCode(code) && attempts < 10);
            registration.setRegCode(code);
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

        if (registration.getDevoteeCount() != null) {
            registration.setDevoteeCount(Math.max(1, registration.getDevoteeCount()));
        } else {
            registration.setDevoteeCount(1);
        }

        if (registration.getBookingFee() <= 0.0) {
            registration.setPaymentStatus("FREE");
            registration.setPaymentMethod("Free Seva");
        } else if (registration.getPaymentStatus() == null) {
            registration.setPaymentStatus("PAID");
        }

        // Auto-populate poojaSevaTimeSlotsId from the schedule when the booking engine path is used
        if (registration.getPoojaSevaTimeSlotsId() == null && registration.getScheduleId() != null) {
            scheduleRepository.findById(registration.getScheduleId())
                    .map(EventPoojaSchedule::getTimeSlotConfigId)
                    .ifPresent(registration::setPoojaSevaTimeSlotsId);
        }
        if (registration.getScheduleId() == null && registration.getPoojaSevaTimeSlotsId() != null) {
            scheduleRepository.findByTimeSlotConfigId(registration.getPoojaSevaTimeSlotsId())
                    .map(EventPoojaSchedule::getId)
                    .ifPresent(registration::setScheduleId);
        }

        // Populate token number from reservation before first save (L-2)
        if (registration.getTokenNumber() == null && registration.getReservationId() != null) {
            slotReservationRepository.findById(registration.getReservationId())
                    .map(EventPoojaSlotReservation::getTokenNumber)
                    .ifPresent(registration::setTokenNumber);
        }

        // Stamp audit / source fields
        boolean isAdmin = isUserAdmin(user);
        if (adminOverride || isAdmin) {
            registration.setRegistrationSource(RegistrationSource.ADMIN);
            if (user != null) registration.setRegisteredBy(user.getId());
        } else {
            registration.setRegistrationSource(RegistrationSource.SELF);
        }
        registration.setOverrideUsed(adminOverride);

        EventPoojaUserRegistration saved = repository.save(registration);

        // Confirm the pre-hold so capacity is counted as confirmed, not reserved
        if (saved.getReservationId() != null) {
            reservationService.confirmReservation(saved.getReservationId(), saved.getId());
        }

        // Materialise participants from the attendingDevotees JSON blob
        syncParticipants(saved, saved.getAttendingDevotees(), saved.getRegCode());

        return saved;
    }

    @Override
    @Transactional
    public EventPoojaUserRegistration updateRegistration(Long id, EventPoojaUserRegistration patch, AppUser user) {
        EventPoojaUserRegistration existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PoojaRegistration", id));

        boolean isAdmin = isUserAdmin(user);
        if (!isAdmin && user != null && existing.getUser() != null && !existing.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You can only update your own pooja registrations.");
        }

        if (patch.getParticipantName() != null && !patch.getParticipantName().isBlank()) {
            existing.setParticipantName(patch.getParticipantName());
        }
        if (patch.getGotram() != null) existing.setGotram(patch.getGotram());
        if (patch.getPhone() != null) existing.setPhone(patch.getPhone());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        if (patch.getFlatNo() != null) existing.setFlatNo(patch.getFlatNo());
        if (patch.getDevoteeCount() != null) existing.setDevoteeCount(patch.getDevoteeCount());
        boolean attendingChanged = patch.getAttendingDevotees() != null
                && !patch.getAttendingDevotees().equals(existing.getAttendingDevotees());
        if (patch.getAttendingDevotees() != null) existing.setAttendingDevotees(patch.getAttendingDevotees());
        if (patch.getPoojaSlotName() != null) existing.setPoojaSlotName(patch.getPoojaSlotName());
        if (patch.getPoojaSlotDate() != null) existing.setPoojaSlotDate(patch.getPoojaSlotDate());
        if (patch.getPoojaSlotTime() != null) existing.setPoojaSlotTime(patch.getPoojaSlotTime());
        if (patch.getVenue() != null) existing.setVenue(patch.getVenue());
        if (patch.getMandap() != null) existing.setMandap(patch.getMandap());
        if (patch.getPanditName() != null) existing.setPanditName(patch.getPanditName());
        if (patch.getBookingFee() != null) existing.setBookingFee(patch.getBookingFee());
        if (patch.getPaymentStatus() != null) existing.setPaymentStatus(patch.getPaymentStatus());
        if (patch.getPaymentMethod() != null) existing.setPaymentMethod(patch.getPaymentMethod());
        if (patch.getPrasadamMode() != null) existing.setPrasadamMode(patch.getPrasadamMode());
        if (patch.getStatus() != null && !patch.getStatus().isBlank()) {
            PoojaRegistrationStatus current = PoojaRegistrationStatus.parse(existing.getStatus(), PoojaRegistrationStatus.CONFIRMED);
            PoojaRegistrationStatus next    = PoojaRegistrationStatus.parse(patch.getStatus(), null);
            if (next == null) {
                throw new IllegalArgumentException("Unknown registration status: " + patch.getStatus());
            }
            if (!isAdmin && !current.canTransitionTo(next)) {
                throw new IllegalStateException(
                        "Invalid status transition: " + current + " → " + next +
                        ". Allowed from " + current + ": " + current.name());
            }
            existing.setStatus(next.name());
        }
        if (patch.getNotes() != null) existing.setNotes(patch.getNotes());
        if (patch.getPoojaSevaTimeSlotsId() != null) existing.setPoojaSevaTimeSlotsId(patch.getPoojaSevaTimeSlotsId());

        EventPoojaUserRegistration saved = repository.save(existing);

        // Re-sync participants when the attendingDevotees blob changed
        if (attendingChanged) {
            syncParticipants(saved, saved.getAttendingDevotees(), saved.getRegCode());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventPoojaUserRegistration> getMyRegistrations(AppUser user, Long communityId) {
        if (user == null) return List.of();
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventPoojaUserRegistration> getRegistrationsByCommunity(Long communityId) {
        if (communityId != null) {
            return repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        }
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public EventPoojaUserRegistration getRegistrationById(Long id, AppUser user) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PoojaRegistration", id));
    }

    @Override
    @Transactional
    public void cancelRegistration(Long id, AppUser user) {
        EventPoojaUserRegistration existing = getRegistrationById(id, user);
        PoojaRegistrationStatus current = PoojaRegistrationStatus.parse(existing.getStatus(), PoojaRegistrationStatus.CONFIRMED);
        if (!current.canTransitionTo(PoojaRegistrationStatus.CANCELLED)) {
            throw new IllegalStateException(
                    "Cannot cancel a registration in status '" + current + "'. " +
                    "Only RESERVED, PAYMENT_PENDING, CONFIRMED, CHECKED_IN, and IN_PROGRESS bookings may be cancelled.");
        }
        existing.setStatus(PoojaRegistrationStatus.CANCELLED.name());
        repository.save(existing);

        // Release the capacity hold so the slot becomes available again
        if (existing.getReservationId() != null) {
            reservationService.releaseReservation(existing.getReservationId());
        }
    }

    @Override
    @Transactional
    public void deleteRegistration(Long id, AppUser user) {
        EventPoojaUserRegistration existing = getRegistrationById(id, user);
        repository.delete(existing);
    }

    @Override
    @Transactional
    public EventPoojaUserRegistration reschedule(Long registrationId, Long newScheduleId, String idempotencyKey, AppUser user) {
        EventPoojaUserRegistration reg = repository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("PoojaRegistration", registrationId));

        PoojaRegistrationStatus current = PoojaRegistrationStatus.parse(reg.getStatus(), PoojaRegistrationStatus.CONFIRMED);
        if (!current.isReschedulable()) {
            throw new IllegalStateException(
                    "Cannot reschedule a registration in status '" + current + "'. " +
                    "Rescheduling is only allowed for CONFIRMED bookings. " +
                    "CHECKED_IN, IN_PROGRESS, and COMPLETED bookings cannot be moved.");
        }

        // Release old slot capacity hold
        if (reg.getReservationId() != null) {
            reservationService.releaseReservation(reg.getReservationId());
        }

        // Reserve the new slot atomically
        PoojaReserveRequest req = new PoojaReserveRequest();
        req.setIdempotencyKey(idempotencyKey != null ? idempotencyKey :
                java.util.UUID.randomUUID().toString());
        req.setFamilyCount(1);
        req.setDevoteeCount(reg.getDevoteeCount() != null ? reg.getDevoteeCount() : 1);
        PoojaReserveResponse newReservation = reservationService.reserve(newScheduleId, req, user);

        // Confirm the new reservation immediately (no payment-pending state for reschedule)
        reservationService.confirmReservation(newReservation.getReservationId(), registrationId);

        // Update registration with new schedule details
        reg.setScheduleId(newScheduleId);
        reg.setReservationId(newReservation.getReservationId());
        reg.setTokenNumber(newReservation.getTokenNumber());
        scheduleRepository.findById(newScheduleId).ifPresent(sch -> {
            reg.setPoojaSlotDate(sch.getScheduleDate().toString());
            reg.setPoojaSlotTime(sch.getStartTime().toString());
            // M-4: keep poojaSevaTimeSlotsId in sync with the new schedule's time-slot config
            if (sch.getTimeSlotConfigId() != null) {
                reg.setPoojaSevaTimeSlotsId(sch.getTimeSlotConfigId());
            }
        });

        return repository.save(reg);
    }

    // ── Participant helpers ────────────────────────────────────────────────────

    /**
     * Delete existing participant rows for this registration and re-insert from the
     * attendingDevotees blob. Idempotent: safe to call on create and on update.
     */
    private void syncParticipants(EventPoojaUserRegistration reg, String attendingDevotees, String regCode) {
        if (attendingDevotees == null || attendingDevotees.isBlank()) return;

        participantRepository.deleteByRegistrationId(reg.getId());

        List<EventPoojaBookingParticipant> rows = parseParticipants(attendingDevotees, reg, regCode);
        if (!rows.isEmpty()) {
            participantRepository.saveAll(rows);
        }
    }

    /**
     * Parses the attendingDevotees string into participant entities. Handles three formats:
     * <ul>
     *   <li>Comma-separated plain names: {@code "Ramesh, Sita, Lakshman"}</li>
     *   <li>JSON array of strings: {@code ["Ramesh","Sita"]}</li>
     *   <li>JSON array of objects: {@code [{"name":"Ramesh","gotram":"Kasyapa","nakshatra":"Rohini","relation":"head"}]}</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private List<EventPoojaBookingParticipant> parseParticipants(
            String attendingDevotees, EventPoojaUserRegistration reg, String regCode) {

        List<EventPoojaBookingParticipant> result = new ArrayList<>();
        String trimmed = attendingDevotees.trim();

        if (trimmed.startsWith("[")) {
            try {
                List<Object> raw = objectMapper.readValue(trimmed, new TypeReference<List<Object>>() {});
                for (int i = 0; i < raw.size(); i++) {
                    Object item = raw.get(i);
                    EventPoojaBookingParticipant p;
                    if (item instanceof String name) {
                        p = participant(reg, name.trim(), null, null, null, regCode, i + 1);
                    } else if (item instanceof Map<?,?> map) {
                        p = participant(reg,
                                str(map, "name"),
                                str(map, "gotram"),
                                str(map, "nakshatra"),
                                str(map, "relation"),
                                regCode, i + 1);
                    } else {
                        continue;
                    }
                    if (p.getName() != null && !p.getName().isBlank()) result.add(p);
                }
            } catch (Exception ignored) {
                // Fallback to comma-split if JSON is malformed
                splitByComma(trimmed, reg, regCode, result);
            }
        } else {
            splitByComma(trimmed, reg, regCode, result);
        }
        return result;
    }

    private void splitByComma(String raw, EventPoojaUserRegistration reg, String regCode,
                               List<EventPoojaBookingParticipant> result) {
        String[] parts = raw.split(",");
        for (int i = 0; i < parts.length; i++) {
            String name = parts[i].trim();
            if (!name.isEmpty()) {
                result.add(participant(reg, name, null, null, null, regCode, i + 1));
            }
        }
    }

    private EventPoojaBookingParticipant participant(EventPoojaUserRegistration reg,
                                                     String name, String gotram,
                                                     String nakshatra, String relation,
                                                     String regCode, int ordinal) {
        String qr = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data="
                + (regCode != null ? regCode : "REG") + "-P" + ordinal;
        return EventPoojaBookingParticipant.builder()
                .registration(reg)
                .name(name)
                .gotram(gotram)
                .nakshatra(nakshatra)
                .relation(relation)
                .qrCodeUrl(qr)
                .build();
    }

    private static String str(Map<?,?> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s.trim() : null;
    }
}
