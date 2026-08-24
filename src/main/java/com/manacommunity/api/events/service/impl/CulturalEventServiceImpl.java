package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.service.CulturalEventService;
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
public class CulturalEventServiceImpl implements CulturalEventService {

    private final CulturalEventRepository repository;
    private final EventCommunityRepository eventRepository;
    private final EventBookingRegistrationRepository bookingRepo;

    public CulturalEventServiceImpl(CulturalEventRepository repository,
                                    EventCommunityRepository eventRepository,
                                    EventBookingRegistrationRepository bookingRepo) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.bookingRepo = bookingRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCulturalEvent> getAllCulturalEvents(Long communityId, Long mainEventId) {
        List<EventCulturalEvent> raw;
        if (mainEventId != null) {
            raw = repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        } else {
            raw = repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
        }

        List<EventCulturalEvent> filtered = new java.util.ArrayList<>();
        java.util.Map<Long, Boolean> eventCancelledCache = new java.util.HashMap<>();
        for (EventCulturalEvent c : raw) {
            if (c.getMainEventId() != null) {
                boolean isParentCancelled = eventCancelledCache.computeIfAbsent(c.getMainEventId(), id -> {
                    com.manacommunity.api.events.entity.EventCommunity parent = eventRepository.findById(id).orElse(null);
                    return parent != null && parent.getStatus() == com.manacommunity.api.events.entity.EventCommunity.EventStatus.CANCELLED;
                });
                if (isParentCancelled) {
                    continue;
                }
            }
            filtered.add(c);
        }
        return filtered;
    }

    @Override
    @Transactional(readOnly = true)
    public EventCulturalEvent getCulturalEventById(Long id, Long communityId) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cultural event", id));
    }

    @Override
    public EventCulturalEvent createCulturalEvent(Long communityId, EventCulturalEvent culturalEvent) {
        validateDateWithinParentEvent(culturalEvent.getMainEventId(), culturalEvent.getDate());
        culturalEvent.setCommunityId(communityId);
        return repository.save(culturalEvent);
    }

    @Override
    public EventCulturalEvent updateCulturalEvent(Long id, Long communityId, EventCulturalEvent updated) {
        EventCulturalEvent existing = getCulturalEventById(id, communityId);
        validateDateWithinParentEvent(updated.getMainEventId(), updated.getDate());
        existing.setMainEventId(updated.getMainEventId());
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPerfType(updated.getPerfType());
        existing.setAgeGroup(updated.getAgeGroup());
        existing.setDate(updated.getDate());
        existing.setStartTime(updated.getStartTime());
        existing.setDuration(updated.getDuration());
        existing.setStage(updated.getStage());
        existing.setRequirements(updated.getRequirements());
        existing.setHasBacktrack(updated.getHasBacktrack());
        existing.setHasLiveMusic(updated.getHasLiveMusic());
        return repository.save(existing);
    }

    private void validateDateWithinParentEvent(Long mainEventId, LocalDate date) {
        if (mainEventId == null || date == null) return;
        eventRepository.findById(mainEventId).ifPresent(event -> {
            LocalDate eventStart = event.getStartDate();
            LocalDate eventEnd = event.getEndDate() != null ? event.getEndDate() : eventStart;
            if (date.isBefore(eventStart) || date.isAfter(eventEnd)) {
                throw new InvalidInputException("Cultural activity date " + date + " is outside the event period (" + eventStart + " to " + eventEnd + ")");
            }
        });
    }

    @Override
    public void deleteCulturalEvent(Long id, Long communityId) {
        EventCulturalEvent existing = getCulturalEventById(id, communityId);
        String actId1 = "cultural-" + existing.getId();
        String actId2 = "cult-" + existing.getId();
        if (bookingRepo.existsByActivityIdAndStatusNot(actId1, "CANCELLED")
                || bookingRepo.existsByActivityIdAndStatusNot(actId2, "CANCELLED")) {
            throw new ManaCommunityException(
                    "Cannot delete cultural event with active bookings. Cancel the bookings first.",
                    HttpStatus.CONFLICT,
                    "CULTURAL_HAS_BOOKINGS"
            );
        }
        repository.delete(existing);
    }
}
