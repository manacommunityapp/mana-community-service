package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventLunchDinner;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.service.LunchDinnerService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LunchDinnerServiceImpl implements LunchDinnerService {

    private final LunchDinnerRepository repository;
    private final EventCommunityRepository eventRepository;
    private final EventBookingRegistrationRepository bookingRepo;

    public LunchDinnerServiceImpl(LunchDinnerRepository repository,
                                  EventCommunityRepository eventRepository,
                                  EventBookingRegistrationRepository bookingRepo) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.bookingRepo = bookingRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventLunchDinner> getAllLunchDinners(Long communityId, Long mainEventId) {
        List<EventLunchDinner> raw;
        if (mainEventId != null) {
            raw = repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        } else {
            raw = repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
        }

        List<EventLunchDinner> filtered = new java.util.ArrayList<>();
        java.util.Map<Long, Boolean> eventCancelledCache = new java.util.HashMap<>();
        for (EventLunchDinner m : raw) {
            if (m.getMainEventId() != null) {
                boolean isParentCancelled = eventCancelledCache.computeIfAbsent(m.getMainEventId(), id -> {
                    com.manacommunity.api.events.entity.EventCommunity parent = eventRepository.findById(id).orElse(null);
                    return parent != null && parent.getStatus() == com.manacommunity.api.events.entity.EventCommunity.EventStatus.CANCELLED;
                });
                if (isParentCancelled) {
                    continue;
                }
            }
            filtered.add(m);
        }
        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public EventLunchDinner getLunchDinnerById(Long id, Long communityId) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lunch/Dinner event", id));
    }

    @Override
    public EventLunchDinner createLunchDinner(Long communityId, EventLunchDinner lunchDinner) {
        validateDateWithinParentEvent(lunchDinner.getMainEventId(), lunchDinner.getDate());
        lunchDinner.setCommunityId(communityId);
        return repository.save(lunchDinner);
    }

    @Override
    public EventLunchDinner updateLunchDinner(Long id, Long communityId, EventLunchDinner updated) {
        EventLunchDinner existing = getLunchDinnerById(id, communityId);
        validateDateWithinParentEvent(updated.getMainEventId(), updated.getDate());
        existing.setMainEventId(updated.getMainEventId());
        existing.setName(updated.getName());
        existing.setMealType(updated.getMealType());
        existing.setDate(updated.getDate());
        existing.setStartTime(updated.getStartTime());
        existing.setEndTime(updated.getEndTime());
        existing.setVenue(updated.getVenue());
        existing.setTargetPlates(updated.getTargetPlates());
        existing.setCaterer(updated.getCaterer());
        existing.setDietType(updated.getDietType());
        existing.setFee(updated.getFee());
        existing.setIsFree(updated.getIsFree());
        if (updated.getMenuItems() != null) {
            existing.setMenuItems(updated.getMenuItems());
        }
        existing.setNotes(updated.getNotes());
        if (updated.getNeedsRegistration() != null) {
            existing.setNeedsRegistration(updated.getNeedsRegistration());
        }
        return repository.save(existing);
    }


    private void validateDateWithinParentEvent(Long mainEventId, LocalDate date) {
        if (mainEventId == null || date == null) return;
        eventRepository.findById(mainEventId).ifPresent(event -> {
            LocalDate eventStart = event.getStartDate();
            LocalDate eventEnd = event.getEndDate() != null ? event.getEndDate() : eventStart;
            if (date.isBefore(eventStart) || date.isAfter(eventEnd)) {
                throw new InvalidInputException("Lunch/Dinner date " + date + " is outside the event period (" + eventStart + " to " + eventEnd + ")");
            }
        });
    }

    @Override
    public void deleteLunchDinner(Long id, Long communityId) {
        EventLunchDinner existing = getLunchDinnerById(id, communityId);
        if (bookingRepo.existsByActivityIdAndStatusNot("food-" + existing.getId(), "CANCELLED")
                || bookingRepo.existsByActivityIdAndStatusNot("meal-" + existing.getId(), "CANCELLED")) {
            throw new ManaCommunityException(
                    "Cannot delete lunch/dinner event with active bookings. Cancel the bookings first.",
                    HttpStatus.CONFLICT,
                    "FOOD_HAS_BOOKINGS"
            );
        }
        repository.delete(existing);
    }
}
