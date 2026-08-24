package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import com.manacommunity.api.events.repository.EventPoojaUserRegistrationRepository;
import com.manacommunity.api.events.service.EventPoojaUserRegistrationService;
import com.manacommunity.api.events.service.PoojaSlotReservationService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class EventPoojaUserRegistrationServiceImpl implements EventPoojaUserRegistrationService {

    private final EventPoojaUserRegistrationRepository repository;
    private final CommunityRepository communityRepository;
    private final PoojaSlotReservationService reservationService;

    public EventPoojaUserRegistrationServiceImpl(
            EventPoojaUserRegistrationRepository repository,
            CommunityRepository communityRepository,
            PoojaSlotReservationService reservationService) {
        this.repository = repository;
        this.communityRepository = communityRepository;
        this.reservationService = reservationService;
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

        if (registration.getRegCode() == null || registration.getRegCode().isBlank()) {
            String code = "MNA-2026-POOJ-" + (1000 + new Random().nextInt(9000));
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

        EventPoojaUserRegistration saved = repository.save(registration);

        // Confirm the pre-hold so capacity is counted as confirmed, not reserved
        if (saved.getReservationId() != null) {
            reservationService.confirmReservation(saved.getReservationId(), saved.getId());
        }

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
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        if (patch.getNotes() != null) existing.setNotes(patch.getNotes());

        return repository.save(existing);
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
        existing.setStatus("CANCELLED");
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
}
