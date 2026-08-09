package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventMediaDayRequest;
import com.manacommunity.api.events.dto.EventMediaDayResponse;
import com.manacommunity.api.events.entity.EventMediaDay;
import com.manacommunity.api.events.repository.EventMediaDayRepository;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventMediaDayService {

    private final EventMediaDayRepository dayRepo;

    @Transactional(readOnly = true)
    public List<EventMediaDayResponse> getAll(Long communityId) {
        return dayRepo.findByCommunityIdOrderBySortOrderAscLabelAsc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventMediaDayResponse create(EventMediaDayRequest req, Community community) {
        int order = req.getSortOrder() != null ? req.getSortOrder()
                : (int) dayRepo.countByCommunityId(community.getId());
        EventMediaDay day = EventMediaDay.builder()
                .community(community)
                .label(req.getLabel().trim())
                .sortOrder(order)
                .build();
        return toResponse(dayRepo.save(day));
    }

    @Transactional
    public void delete(Long id, Long communityId) {
        EventMediaDay day = dayRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Day tag not found: " + id));
        if (!day.getCommunity().getId().equals(communityId)) {
            throw new IllegalStateException("Not authorized to delete this day tag");
        }
        dayRepo.delete(day);
    }

    private EventMediaDayResponse toResponse(EventMediaDay d) {
        return EventMediaDayResponse.builder()
                .id(d.getId())
                .label(d.getLabel())
                .sortOrder(d.getSortOrder())
                .build();
    }
}
