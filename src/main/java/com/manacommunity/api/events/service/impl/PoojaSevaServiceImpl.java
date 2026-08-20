package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.PoojaSeva;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.events.service.PoojaSevaService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PoojaSevaServiceImpl implements PoojaSevaService {

    private final PoojaSevaRepository repository;
    private final CommunityEventRepository eventRepository;

    public PoojaSevaServiceImpl(PoojaSevaRepository repository, CommunityEventRepository eventRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PoojaSeva> getAllPoojaSevas(Long communityId, Long mainEventId) {
        if (communityId != null && mainEventId != null) {
            return repository.findByCommunityIdAndMainEventIdOrderByDateAscStartTimeAsc(communityId, mainEventId);
        } else if (mainEventId != null) {
            return repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        }
        return repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
    }

    @Override
    @Transactional(readOnly = true)
    public PoojaSeva getPoojaSevaById(Long id, Long communityId) {
        if (communityId != null) {
            return repository.findByIdAndCommunityId(id, communityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
    }

    @Override
    public PoojaSeva createPoojaSeva(Long communityId, PoojaSeva poojaSeva) {
        if (poojaSeva.getMainEventId() != null) {
            eventRepository.findById(poojaSeva.getMainEventId()).ifPresentOrElse(parentEvent -> {
                if (communityId != null && parentEvent.getCommunity() != null
                        && !communityId.equals(parentEvent.getCommunity().getId())) {
                    throw new IllegalArgumentException("Parent event belongs to a different community");
                }
            }, () -> {
                throw new ResourceNotFoundException("Parent Event", poojaSeva.getMainEventId());
            });
        }
        poojaSeva.setCommunityId(communityId);
        return repository.save(poojaSeva);
    }

    @Override
    public PoojaSeva updatePoojaSeva(Long id, Long communityId, PoojaSeva updated) {
        PoojaSeva existing = getPoojaSevaById(id, communityId);
        if (updated.getMainEventId() != null) {
            eventRepository.findById(updated.getMainEventId()).ifPresentOrElse(parentEvent -> {
                if (communityId != null && parentEvent.getCommunity() != null
                        && !communityId.equals(parentEvent.getCommunity().getId())) {
                    throw new IllegalArgumentException("Parent event belongs to a different community");
                }
            }, () -> {
                throw new ResourceNotFoundException("Parent Event", updated.getMainEventId());
            });
        }
        existing.setMainEventId(updated.getMainEventId());
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setDate(updated.getDate());
        existing.setEndDate(updated.getEndDate());
        existing.setMultiDay(updated.getMultiDay());
        existing.setStartTime(updated.getStartTime());
        existing.setDuration(updated.getDuration());
        existing.setMandap(updated.getMandap());
        existing.setPandit(updated.getPandit());
        existing.setSlots(updated.getSlots());
        existing.setFee(updated.getFee());
        existing.setIsFree(updated.getIsFree());
        if (updated.getItems() != null) {
            existing.getItems().clear();
            existing.getItems().addAll(updated.getItems());
        }
        if (updated.getStartTimes() != null) {
            existing.getStartTimes().clear();
            existing.getStartTimes().addAll(updated.getStartTimes());
        }
        if (updated.getDaySlots() != null) {
            existing.getDaySlots().clear();
            existing.getDaySlots().addAll(updated.getDaySlots());
        }
        if (updated.getTimeSlotConfig() != null) {
            existing.getTimeSlotConfig().clear();
            existing.getTimeSlotConfig().addAll(updated.getTimeSlotConfig());
        }
        existing.setNotes(updated.getNotes());
        return repository.save(existing);
    }

    @Override
    public void deletePoojaSeva(Long id, Long communityId) {
        PoojaSeva existing = getPoojaSevaById(id, communityId);
        repository.delete(existing);
    }
}
