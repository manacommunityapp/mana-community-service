package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.LunchDinner;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.service.LunchDinnerService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LunchDinnerServiceImpl implements LunchDinnerService {

    private final LunchDinnerRepository repository;

    public LunchDinnerServiceImpl(LunchDinnerRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchDinner> getAllLunchDinners(Long communityId, Long mainEventId) {
        if (mainEventId != null) {
            return repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        }
        return repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
    }

    @Override
    @Transactional(readOnly = true)
    public LunchDinner getLunchDinnerById(Long id, Long communityId) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lunch/Dinner event", id));
    }

    @Override
    public LunchDinner createLunchDinner(Long communityId, LunchDinner lunchDinner) {
        lunchDinner.setCommunityId(communityId);
        return repository.save(lunchDinner);
    }

    @Override
    public LunchDinner updateLunchDinner(Long id, Long communityId, LunchDinner updated) {
        LunchDinner existing = getLunchDinnerById(id, communityId);
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
        return repository.save(existing);
    }

    @Override
    public void deleteLunchDinner(Long id, Long communityId) {
        LunchDinner existing = getLunchDinnerById(id, communityId);
        repository.delete(existing);
    }
}
