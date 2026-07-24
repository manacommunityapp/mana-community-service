package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodEvent;
import com.manacommunity.api.food.entity.FoodEventContribution;
import com.manacommunity.api.food.entity.FoodEventFeedback;
import com.manacommunity.api.food.entity.FoodEventRegistration;
import com.manacommunity.api.food.repository.FoodEventContributionRepository;
import com.manacommunity.api.food.repository.FoodEventFeedbackRepository;
import com.manacommunity.api.food.repository.FoodEventRegistrationRepository;
import com.manacommunity.api.food.repository.FoodEventRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodEventService {

    private final FoodEventRepository eventRepo;
    private final FoodEventRegistrationRepository registrationRepo;
    private final FoodEventContributionRepository contributionRepo;
    private final FoodEventFeedbackRepository feedbackRepo;

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getEvents(Long communityId, String status, Pageable pageable) {
        Page<FoodEvent> events;
        if (status != null && !status.isBlank()) {
            events = eventRepo.findByCommunityIdAndStatus(communityId, status, pageable);
        } else {
            events = eventRepo.findByCommunityIdAndStatus(communityId, null, pageable);
        }
        return events.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEventById(Long id, Long communityId) {
        FoodEvent event = eventRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodEvent", id));
        return toResponse(event);
    }

    @Transactional
    public Map<String, Object> createEvent(Map<String, Object> request, AppUser user, Community community) {
        FoodEvent event = FoodEvent.builder()
                .name((String) request.get("name"))
                .description((String) request.get("description"))
                .eventType(request.containsKey("eventType")
                        ? FoodEvent.FoodEventType.valueOf((String) request.get("eventType")) : null)
                .venue((String) request.get("venue"))
                .date(request.containsKey("date") ? LocalDate.parse((String) request.get("date")) : null)
                .startTime(request.containsKey("startTime") ? LocalTime.parse((String) request.get("startTime")) : null)
                .endTime(request.containsKey("endTime") ? LocalTime.parse((String) request.get("endTime")) : null)
                .capacity(request.containsKey("capacity") ? (Integer) request.get("capacity") : null)
                .registered(0)
                .price(request.containsKey("price") ? new BigDecimal(request.get("price").toString()) : null)
                .imageUrl((String) request.get("imageUrl"))
                .organizer(user)
                .status(FoodEvent.FoodEventStatus.DRAFT)
                .registrationDeadline(request.containsKey("registrationDeadline")
                        ? LocalDateTime.parse((String) request.get("registrationDeadline")) : null)
                .community(community)
                .build();

        FoodEvent saved = eventRepo.save(event);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> updateEvent(Long id, Map<String, Object> request, Long communityId) {
        FoodEvent event = eventRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodEvent", id));

        if (request.containsKey("name")) {
            event.setName((String) request.get("name"));
        }
        if (request.containsKey("description")) {
            event.setDescription((String) request.get("description"));
        }
        if (request.containsKey("eventType")) {
            event.setEventType(FoodEvent.FoodEventType.valueOf((String) request.get("eventType")));
        }
        if (request.containsKey("venue")) {
            event.setVenue((String) request.get("venue"));
        }
        if (request.containsKey("date")) {
            event.setDate(LocalDate.parse((String) request.get("date")));
        }
        if (request.containsKey("startTime")) {
            event.setStartTime(LocalTime.parse((String) request.get("startTime")));
        }
        if (request.containsKey("endTime")) {
            event.setEndTime(LocalTime.parse((String) request.get("endTime")));
        }
        if (request.containsKey("capacity")) {
            event.setCapacity((Integer) request.get("capacity"));
        }
        if (request.containsKey("price")) {
            event.setPrice(new BigDecimal(request.get("price").toString()));
        }
        if (request.containsKey("imageUrl")) {
            event.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("status")) {
            event.setStatus(FoodEvent.FoodEventStatus.valueOf((String) request.get("status")));
        }
        if (request.containsKey("registrationDeadline")) {
            event.setRegistrationDeadline(LocalDateTime.parse((String) request.get("registrationDeadline")));
        }

        FoodEvent saved = eventRepo.save(event);
        return toResponse(saved);
    }

    @Transactional
    public Map<String, Object> registerForEvent(Long eventId, Integer guests, String dietaryReqs,
                                                 AppUser user, Community community) {
        FoodEvent event = eventRepo.findByIdAndCommunityId(eventId, community.getId())
                .orElseThrow(() -> new ResourceNotFoundException("FoodEvent", eventId));

        String qrCode = UUID.randomUUID().toString();
        int totalGuests = 1 + (guests != null ? guests : 0);
        BigDecimal totalAmount = event.getPrice() != null
                ? event.getPrice().multiply(BigDecimal.valueOf(totalGuests)) : BigDecimal.ZERO;

        FoodEventRegistration registration = FoodEventRegistration.builder()
                .event(event)
                .user(user)
                .guests(guests != null ? guests : 0)
                .totalAmount(totalAmount)
                .status(FoodEventRegistration.RegistrationStatus.REGISTERED)
                .qrCode(qrCode)
                .dietaryRequirements(dietaryReqs)
                .community(community)
                .build();

        FoodEventRegistration saved = registrationRepo.save(registration);

        event.setRegistered(event.getRegistered() + totalGuests);
        eventRepo.save(event);

        return toRegistrationResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getRegistrations(Long eventId, Pageable pageable) {
        Page<FoodEventRegistration> registrations = registrationRepo.findByEventId(eventId, pageable);
        return registrations.map(this::toRegistrationResponse);
    }

    @Transactional
    public Map<String, Object> checkInAttendee(Long registrationId, Long communityId) {
        FoodEventRegistration registration = registrationRepo.findByIdAndCommunityId(registrationId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodEventRegistration", registrationId));

        registration.setStatus(FoodEventRegistration.RegistrationStatus.ATTENDED);
        registration.setCheckedInAt(LocalDateTime.now());

        FoodEventRegistration saved = registrationRepo.save(registration);
        return toRegistrationResponse(saved);
    }

    @Transactional
    public Map<String, Object> addContribution(Long eventId, Map<String, Object> request, AppUser user) {
        FoodEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodEvent", eventId));

        FoodEventContribution contribution = FoodEventContribution.builder()
                .event(event)
                .user(user)
                .itemName((String) request.get("itemName"))
                .itemType(request.containsKey("itemType")
                        ? FoodEventContribution.ContributionType.valueOf((String) request.get("itemType")) : null)
                .quantity((String) request.get("quantity"))
                .servingSize((String) request.get("servingSize"))
                .isVeg(request.containsKey("isVeg") ? (Boolean) request.get("isVeg") : null)
                .allergens((String) request.get("allergens"))
                .community(event.getCommunity())
                .build();

        FoodEventContribution saved = contributionRepo.save(contribution);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("eventId", saved.getEvent().getId());
        map.put("userId", saved.getUser().getId());
        map.put("itemName", saved.getItemName());
        map.put("itemType", saved.getItemType() != null ? saved.getItemType().name() : null);
        map.put("quantity", saved.getQuantity());
        map.put("servingSize", saved.getServingSize());
        map.put("isVeg", saved.getIsVeg());
        map.put("allergens", saved.getAllergens());
        map.put("communityId", saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        map.put("createdAt", saved.getCreatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> submitFeedback(Long eventId, Map<String, Object> request, AppUser user) {
        FoodEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodEvent", eventId));

        FoodEventFeedback feedback = FoodEventFeedback.builder()
                .event(event)
                .user(user)
                .rating(request.containsKey("rating") ? (Integer) request.get("rating") : null)
                .foodRating(request.containsKey("foodRating") ? (Integer) request.get("foodRating") : null)
                .organizationRating(request.containsKey("organizationRating") ? (Integer) request.get("organizationRating") : null)
                .venueRating(request.containsKey("venueRating") ? (Integer) request.get("venueRating") : null)
                .comments((String) request.get("comments"))
                .suggestions((String) request.get("suggestions"))
                .community(event.getCommunity())
                .build();

        FoodEventFeedback saved = feedbackRepo.save(feedback);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("eventId", saved.getEvent().getId());
        map.put("userId", saved.getUser().getId());
        map.put("rating", saved.getRating());
        map.put("foodRating", saved.getFoodRating());
        map.put("organizationRating", saved.getOrganizationRating());
        map.put("venueRating", saved.getVenueRating());
        map.put("comments", saved.getComments());
        map.put("suggestions", saved.getSuggestions());
        map.put("communityId", saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        map.put("createdAt", saved.getCreatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("name", event.getName());
        map.put("description", event.getDescription());
        map.put("eventType", event.getEventType() != null ? event.getEventType().name() : null);
        map.put("venue", event.getVenue());
        map.put("date", event.getDate());
        map.put("startTime", event.getStartTime());
        map.put("endTime", event.getEndTime());
        map.put("capacity", event.getCapacity());
        map.put("registered", event.getRegistered());
        map.put("price", event.getPrice());
        map.put("imageUrl", event.getImageUrl());
        map.put("organizerId", event.getOrganizer() != null ? event.getOrganizer().getId() : null);
        map.put("status", event.getStatus() != null ? event.getStatus().name() : null);
        map.put("registrationDeadline", event.getRegistrationDeadline());
        map.put("communityId", event.getCommunity() != null ? event.getCommunity().getId() : null);
        map.put("createdAt", event.getCreatedAt());
        map.put("updatedAt", event.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toRegistrationResponse(FoodEventRegistration reg) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reg.getId());
        map.put("eventId", reg.getEvent().getId());
        map.put("userId", reg.getUser().getId());
        map.put("guests", reg.getGuests());
        map.put("totalAmount", reg.getTotalAmount());
        map.put("status", reg.getStatus() != null ? reg.getStatus().name() : null);
        map.put("qrCode", reg.getQrCode());
        map.put("checkedInAt", reg.getCheckedInAt());
        map.put("dietaryRequirements", reg.getDietaryRequirements());
        map.put("contributionItem", reg.getContributionItem());
        map.put("communityId", reg.getCommunity() != null ? reg.getCommunity().getId() : null);
        map.put("createdAt", reg.getCreatedAt());
        map.put("updatedAt", reg.getUpdatedAt());
        return map;
    }
}
