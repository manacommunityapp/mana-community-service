package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.service.EventBookingRegistrationService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class EventBookingRegistrationServiceImpl implements EventBookingRegistrationService {

    private final EventBookingRegistrationRepository repository;
    private final CommunityRepository communityRepository;

    public EventBookingRegistrationServiceImpl(EventBookingRegistrationRepository repository, CommunityRepository communityRepository) {
        this.repository = repository;
        this.communityRepository = communityRepository;
    }

    @Override
    @Transactional
    public EventBookingRegistration createRegistration(EventBookingRegistration registration, AppUser user, Long communityId) {
        Community comm = (user != null && user.getCommunity() != null)
                ? user.getCommunity()
                : (communityId != null ? communityRepository.findById(communityId).orElse(null) : null);

        registration.setId(null);
        registration.setUser(user);
        registration.setCommunity(comm);

        if (registration.getRegCode() == null || registration.getRegCode().isBlank()) {
            String cat = (registration.getCategory() != null && !registration.getCategory().isBlank())
                    ? registration.getCategory().toUpperCase().replaceAll("[^A-Z]", "")
                    : "EVT";
            if (cat.length() > 4) {
                cat = cat.substring(0, 4);
            }
            int rand = 1000 + new Random().nextInt(9000);
            registration.setRegCode("MNA-2026-" + cat + "-" + rand);
        }

        if (registration.getQrCodeUrl() == null || registration.getQrCodeUrl().isBlank()) {
            registration.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" + registration.getRegCode());
        }

        if (registration.getStatus() == null || registration.getStatus().isBlank()) {
            registration.setStatus("CONFIRMED");
        }

        if (registration.getPaymentStatus() == null || registration.getPaymentStatus().isBlank()) {
            registration.setPaymentStatus((registration.getBookingFee() != null && registration.getBookingFee() > 0) ? "PAID" : "FREE");
        }

        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());

        return repository.save(registration);
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
        repository.save(reg);
    }
}
