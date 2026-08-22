package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CulturalEvent;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.service.CulturalEventService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CulturalEventServiceImpl implements CulturalEventService {

    private final CulturalEventRepository repository;
    private final CommunityEventRepository eventRepository;

    public CulturalEventServiceImpl(CulturalEventRepository repository, CommunityEventRepository eventRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CulturalEvent> getAllCulturalEvents(Long communityId, Long mainEventId) {
        if (mainEventId != null) {
            return repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        }
        return repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
    }

    @Override
    @Transactional(readOnly = true)
    public CulturalEvent getCulturalEventById(Long id, Long communityId) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cultural event", id));
    }

    @Override
    public CulturalEvent createCulturalEvent(Long communityId, CulturalEvent culturalEvent) {
        validateDateWithinParentEvent(culturalEvent.getMainEventId(), culturalEvent.getDate());
        culturalEvent.setCommunityId(communityId);
        return repository.save(culturalEvent);
    }

    @Override
    public CulturalEvent updateCulturalEvent(Long id, Long communityId, CulturalEvent updated) {
        CulturalEvent existing = getCulturalEventById(id, communityId);
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
        CulturalEvent existing = getCulturalEventById(id, communityId);
        repository.delete(existing);
    }
}
