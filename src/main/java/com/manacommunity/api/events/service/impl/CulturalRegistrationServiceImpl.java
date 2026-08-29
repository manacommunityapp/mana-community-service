package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.entity.EventCulturalRegistration;
import com.manacommunity.api.events.enums.RegistrationSource;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.CulturalRegistrationRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.service.CulturalRegistrationService;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.RegistrationClosedException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class CulturalRegistrationServiceImpl implements CulturalRegistrationService {

    private static final int DEFAULT_CAPACITY = 50;

    private static String regCodePrefix() {
        return "MNA-" + java.time.LocalDate.now().getYear() + "-CULT-";
    }

    private final CulturalRegistrationRepository repository;
    private final CulturalEventRepository culturalEventRepository;
    private final EventCommunityRepository communityEventRepository;

    public CulturalRegistrationServiceImpl(
            CulturalRegistrationRepository repository,
            CulturalEventRepository culturalEventRepository,
            EventCommunityRepository communityEventRepository) {
        this.repository = repository;
        this.culturalEventRepository = culturalEventRepository;
        this.communityEventRepository = communityEventRepository;
    }

    @Override
    @Transactional
    public EventCulturalRegistration createRegistration(
            EventCulturalRegistration request, AppUser caller, Long communityId, boolean adminOverride) {

        Long culturalEventId = request.getCulturalEventId();
        if (culturalEventId == null) {
            throw new InvalidInputException("culturalEventId is required");
        }

        EventCulturalEvent event = culturalEventRepository.findById(culturalEventId)
                .orElseThrow(() -> new ResourceNotFoundException("EventCulturalEvent", culturalEventId));

        // Guard: registration must be enabled for this activity
        if (Boolean.FALSE.equals(event.getNeedsRegistration())) {
            throw new InvalidInputException("Registration is not required for this cultural performance.");
        }

        // Guard: cultural event itself must not be cancelled
        if (event.getStatus() == EventCulturalEvent.CulturalEventStatus.CANCELLED) {
            throw new RegistrationClosedException(event.getName(), "This cultural event has been cancelled.");
        }

        // Guard: parent community event must not be cancelled
        if (event.getMainEventId() != null) {
            EventCommunity parent = communityEventRepository.findById(event.getMainEventId()).orElse(null);
            if (parent != null && parent.getStatus() == EventCommunity.EventStatus.CANCELLED) {
                throw new RegistrationClosedException(event.getName(), "Parent event has been cancelled.");
            }
        }

        // Guard: registration deadline must not have passed
        if (event.getRegDeadline() != null && LocalDate.now().isAfter(event.getRegDeadline())) {
            throw new RegistrationClosedException(event.getName(),
                    "Registration deadline has passed (" + event.getRegDeadline() + ").");
        }

        // Guard: event date must not have passed
        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now())) {
            throw new RegistrationClosedException(event.getName(),
                    "Cultural event date has passed (" + event.getDate() + ").");
        }

        boolean isAdmin = isUserAdmin(caller) || adminOverride;

        // Resolve effective user
        Long effectiveUserId = caller != null ? caller.getId() : null;
        if (request.getUserId() != null) {
            effectiveUserId = request.getUserId();
        }

        if (!isAdmin && effectiveUserId != null) {
            // Duplicate check
            if (repository.existsByUserIdAndCulturalEventIdAndStatusNot(effectiveUserId, culturalEventId, "CANCELLED")) {
                throw new AlreadyRegisteredException(event.getName(), "You are already registered for this cultural event.");
            }

            // Capacity check — read from entity; fall back to DEFAULT_CAPACITY
            int maxEntries = (event.getCapacity() != null && event.getCapacity() > 0)
                    ? event.getCapacity() : DEFAULT_CAPACITY;
            long booked = repository.countByCulturalEventIdAndStatusNot(culturalEventId, "CANCELLED");
            int requested = request.getDevoteeCount() != null && request.getDevoteeCount() > 0
                    ? request.getDevoteeCount() : 1;
            if (booked + requested > maxEntries) {
                throw new EventFullException(
                        event.getName() + " (Capacity: " + maxEntries + ", Booked: " + booked + ")", maxEntries);
            }
        }

        // Resolve communityId
        Long resolvedCommunityId = request.getCommunityId();
        if (resolvedCommunityId == null) {
            if (communityId != null) {
                resolvedCommunityId = communityId;
            } else if (caller != null && caller.getCommunity() != null) {
                resolvedCommunityId = caller.getCommunity().getId();
            } else {
                resolvedCommunityId = event.getCommunityId();
            }
        }

        // Build registration
        request.setId(null);
        request.setUserId(effectiveUserId);
        request.setCommunityId(resolvedCommunityId);
        request.setMainEventId(event.getMainEventId());

        String rnd = String.format("%06d", new Random().nextInt(900000) + 100000);
        request.setRegCode(regCodePrefix() + rnd);
        request.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + request.getRegCode());

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("CONFIRMED");
        }
        if (request.getDevoteeCount() == null || request.getDevoteeCount() < 1) {
            request.setDevoteeCount(1);
        }

        if (isAdmin) {
            request.setRegistrationSource(RegistrationSource.ADMIN);
            if (caller != null) request.setRegisteredBy(caller.getId());
        } else {
            request.setRegistrationSource(RegistrationSource.SELF);
        }
        request.setOverrideUsed(adminOverride);

        return repository.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCulturalRegistration> getMyRegistrations(AppUser user, Long communityId) {
        if (user == null) return List.of();
        if (communityId != null) {
            return repository.findByUserIdAndCommunityIdOrderByCreatedAtDesc(user.getId(), communityId);
        }
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCulturalRegistration> getRegistrationsByCommunity(Long communityId) {
        if (communityId == null) return List.of();
        return repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCulturalRegistration> getRegistrationsByCulturalEvent(Long culturalEventId) {
        return repository.findByCulturalEventIdOrderByCreatedAtDesc(culturalEventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCulturalRegistration> getRegistrationsByMainEvent(Long mainEventId) {
        return repository.findByMainEventIdOrderByCreatedAtDesc(mainEventId);
    }

    @Override
    @Transactional(readOnly = true)
    public EventCulturalRegistration getById(Long id, AppUser caller) {
        EventCulturalRegistration reg = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventCulturalRegistration", id));
        if (!isUserAdmin(caller) && (caller == null || !caller.getId().equals(reg.getUserId()))) {
            throw new UnauthorizedActionException("You do not have permission to view this registration.");
        }
        return reg;
    }

    @Override
    @Transactional
    public void cancelRegistration(Long id, String reason, AppUser caller) {
        EventCulturalRegistration reg = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventCulturalRegistration", id));
        if (!isUserAdmin(caller) && (caller == null || !caller.getId().equals(reg.getUserId()))) {
            throw new UnauthorizedActionException("You do not have permission to cancel this registration.");
        }
        reg.setStatus("CANCELLED");
        reg.setCancellationReason(reason);
        reg.setCancelledAt(LocalDateTime.now());
        repository.save(reg);
    }

    @Override
    @Transactional
    public void deleteRegistration(Long id, AppUser caller) {
        EventCulturalRegistration reg = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventCulturalRegistration", id));
        if (!isUserAdmin(caller)) {
            throw new UnauthorizedActionException("Only admins can permanently delete registrations.");
        }
        repository.delete(reg);
    }

    @Override
    @Transactional
    public EventCulturalRegistration checkIn(Long id, AppUser caller) {
        EventCulturalRegistration reg = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventCulturalRegistration", id));
        if (!isUserAdmin(caller)) {
            throw new UnauthorizedActionException("Only admins can check in registrations.");
        }
        if ("CANCELLED".equalsIgnoreCase(reg.getStatus())) {
            throw new InvalidInputException("Cannot check in a cancelled registration.");
        }
        reg.setCheckedIn(true);
        reg.setCheckedInAt(LocalDateTime.now());
        return repository.save(reg);
    }

    private boolean isUserAdmin(AppUser user) {
        if (user == null) return false;
        return user.hasRole("ADMIN") || user.hasRole("SUPER_ADMIN") ||
                user.hasRole("COMMUNITY_ADMIN") || user.hasRole("EVENT_ADMIN") ||
                user.hasRole("ROLE_ADMIN") || user.hasRole("ROLE_SUPER_ADMIN") ||
                user.hasRole("ROLE_COMMUNITY_ADMIN") || user.hasRole("ROLE_EVENT_ADMIN");
    }
}
