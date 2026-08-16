package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.PoojaSeva;
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

    public PoojaSevaServiceImpl(PoojaSevaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PoojaSeva> getAllPoojaSevas(Long communityId, Long mainEventId) {
        if (mainEventId != null) {
            return repository.findByMainEventIdOrderByDateAscStartTimeAsc(mainEventId);
        }
        return repository.findByCommunityIdOrderByDateAscStartTimeAsc(communityId);
    }

    @Override
    @Transactional(readOnly = true)
    public PoojaSeva getPoojaSevaById(Long id, Long communityId) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pooja/Seva", id));
    }

    @Override
    public PoojaSeva createPoojaSeva(Long communityId, PoojaSeva poojaSeva) {
        poojaSeva.setCommunityId(communityId);
        return repository.save(poojaSeva);
    }

    @Override
    public PoojaSeva updatePoojaSeva(Long id, Long communityId, PoojaSeva updated) {
        PoojaSeva existing = getPoojaSevaById(id, communityId);
        existing.setMainEventId(updated.getMainEventId());
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setDate(updated.getDate());
        existing.setStartTime(updated.getStartTime());
        existing.setDuration(updated.getDuration());
        existing.setMandap(updated.getMandap());
        existing.setPandit(updated.getPandit());
        existing.setSlots(updated.getSlots());
        existing.setFee(updated.getFee());
        existing.setIsFree(updated.getIsFree());
        if (updated.getItems() != null) {
            existing.setItems(updated.getItems());
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
