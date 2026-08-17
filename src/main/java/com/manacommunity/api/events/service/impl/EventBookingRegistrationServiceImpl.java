package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.Competition;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.LunchDinner;
import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
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
    private final PoojaSevaRepository poojaSevaRepository;
    private final LunchDinnerRepository lunchDinnerRepository;
    private final CompetitionRepository competitionRepository;
    private final CommunityEventRepository communityEventRepository;

    public EventBookingRegistrationServiceImpl(
            EventBookingRegistrationRepository repository,
            CommunityRepository communityRepository,
            PoojaSevaRepository poojaSevaRepository,
            LunchDinnerRepository lunchDinnerRepository,
            CompetitionRepository competitionRepository,
            CommunityEventRepository communityEventRepository) {
        this.repository = repository;
        this.communityRepository = communityRepository;
        this.poojaSevaRepository = poojaSevaRepository;
        this.lunchDinnerRepository = lunchDinnerRepository;
        this.competitionRepository = competitionRepository;
        this.communityEventRepository = communityEventRepository;
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

        registration.setDevoteeCount(computeDevoteeCount(registration.getDevoteeCount(), registration.getAttendingDevotees(), registration.getMembersJson()));

        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());

        EventBookingRegistration saved = repository.save(registration);

        // Decrement slots / capacity for the booked activity
        decrementActivitySlots(saved);

        return saved;
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
                    int current = p.getSlots() != null ? p.getSlots() : 20;
                    p.setSlots(Math.max(0, current - booked));
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
                    if (ev.getMaxAttendees() != null) {
                        ev.setMaxAttendees(Math.max(0, ev.getMaxAttendees() - booked));
                    }
                    communityEventRepository.save(ev);
                });
            } else {
                try {
                    Long id = Long.parseLong(actId);
                    communityEventRepository.findById(id).ifPresent(ev -> {
                        if (ev.getCapacity() != null) {
                            ev.setCapacity(Math.max(0, ev.getCapacity() - booked));
                        }
                        if (ev.getMaxAttendees() != null) {
                            ev.setMaxAttendees(Math.max(0, ev.getMaxAttendees() - booked));
                        }
                        communityEventRepository.save(ev);
                    });
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ex) {
            // Non-critical slot decrement failure
        }
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

    @Override
    @Transactional
    public EventBookingRegistration updateRegistration(Long id, EventBookingRegistration patch, AppUser user) {
        EventBookingRegistration reg = getRegistrationById(id, user);

        // Only allow update if not already cancelled
        if ("CANCELLED".equalsIgnoreCase(reg.getStatus())) {
            throw new IllegalStateException("Cannot update a cancelled registration");
        }

        // Patch only the mutable fields the user is allowed to change
        if (patch.getParticipantName() != null && !patch.getParticipantName().isBlank()) {
            reg.setParticipantName(patch.getParticipantName().trim());
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
        if (patch.getPaymentReceiptUrl() != null) {
            reg.setPaymentReceiptUrl(patch.getPaymentReceiptUrl());
        }
        if (patch.getTransactionId() != null) {
            reg.setTransactionId(patch.getTransactionId());
        }
        if (patch.getPaymentMethod() != null) {
            reg.setPaymentMethod(patch.getPaymentMethod());
        }

        reg.setUpdatedAt(LocalDateTime.now());
        return repository.save(reg);
    }
}
