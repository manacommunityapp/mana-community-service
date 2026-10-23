package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.CulturalRegistrationRepository;
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

    // Canonical prefix for all new cultural activity booking IDs.
    // "cult-" is the legacy prefix used in bookings created before this was standardised;
    // delete guards must check both until those older records are migrated or aged out.
    static final String ACTIVITY_ID_PREFIX = "cultural-";
    private static final String LEGACY_ACTIVITY_ID_PREFIX = "cult-";

    private final CulturalEventRepository repository;
    private final EventCommunityRepository eventRepository;
    private final EventBookingRegistrationRepository bookingRepo;
    private final CulturalRegistrationRepository culturalRegRepo;

    public CulturalEventServiceImpl(CulturalEventRepository repository,
                                    EventCommunityRepository eventRepository,
                                    EventBookingRegistrationRepository bookingRepo,
                                    CulturalRegistrationRepository culturalRegRepo) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.bookingRepo = bookingRepo;
        this.culturalRegRepo = culturalRegRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCulturalEvent> getAllCulturalEvents(Long communityId, Long mainEventId) {
        List<EventCulturalEvent> raw;
        if (mainEventId != null) {
            raw = repository.findByMainEventIdOrderByDateAscStartTimeAscSortOrderAsc(mainEventId);
        } else {
            raw = repository.findByCommunityIdOrderByDateAscStartTimeAscSortOrderAsc(communityId);
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
        if (updated.getNeedsRegistration() != null) {
            existing.setNeedsRegistration(updated.getNeedsRegistration());
        }
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getCapacity() != null) {
            existing.setCapacity(updated.getCapacity());
        }
        if (updated.getSortOrder() != null) {
            existing.setSortOrder(updated.getSortOrder());
        }
        existing.setRegDeadline(updated.getRegDeadline());
        return repository.save(existing);
    }

    @Override
    public void deleteCulturalEvent(Long id, Long communityId) {
        EventCulturalEvent existing = getCulturalEventById(id, communityId);
        String actId = ACTIVITY_ID_PREFIX + existing.getId();
        String legacyActId = LEGACY_ACTIVITY_ID_PREFIX + existing.getId();
        boolean hasLegacyBookings = bookingRepo.existsByActivityIdAndStatusNot(actId, "CANCELLED")
                || bookingRepo.existsByActivityIdAndStatusNot(legacyActId, "CANCELLED");
        boolean hasDedicatedRegs = culturalRegRepo.countByCulturalEventIdAndStatusNot(existing.getId(), "CANCELLED") > 0;
        if (hasLegacyBookings || hasDedicatedRegs) {
            throw new ManaCommunityException(
                    "Cannot delete cultural event with active registrations. Cancel the registrations first.",
                    HttpStatus.CONFLICT,
                    "CULTURAL_HAS_BOOKINGS"
            );
        }
        repository.delete(existing);
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
}
