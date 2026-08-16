package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CulturalEvent;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.service.CulturalEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CulturalEventServiceImpl implements CulturalEventService {

    private final CulturalEventRepository repository;

    public CulturalEventServiceImpl(CulturalEventRepository repository) {
        this.repository = repository;
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
                .orElseThrow(() -> new IllegalArgumentException("Cultural Event not found with id: " + id));
    }

    @Override
    public CulturalEvent createCulturalEvent(Long communityId, CulturalEvent culturalEvent) {
        culturalEvent.setCommunityId(communityId);
        return repository.save(culturalEvent);
    }

    @Override
    public CulturalEvent updateCulturalEvent(Long id, Long communityId, CulturalEvent updated) {
        CulturalEvent existing = getCulturalEventById(id, communityId);
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

    @Override
    public void deleteCulturalEvent(Long id, Long communityId) {
        CulturalEvent existing = getCulturalEventById(id, communityId);
        repository.delete(existing);
    }
}
