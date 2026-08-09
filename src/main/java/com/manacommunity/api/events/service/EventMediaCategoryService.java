package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventMediaCategoryRequest;
import com.manacommunity.api.events.dto.EventMediaCategoryResponse;
import com.manacommunity.api.events.entity.EventMediaCategory;
import com.manacommunity.api.events.repository.EventMediaCategoryRepository;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventMediaCategoryService {

    private final EventMediaCategoryRepository categoryRepo;

    @Transactional(readOnly = true)
    public List<EventMediaCategoryResponse> getAll(Long communityId) {
        return categoryRepo.findByCommunityIdOrderBySortOrderAscNameAsc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventMediaCategoryResponse create(EventMediaCategoryRequest req, Community community) {
        int order = req.getSortOrder() != null ? req.getSortOrder()
                : (int) categoryRepo.findByCommunityIdOrderBySortOrderAscNameAsc(community.getId()).size();
        EventMediaCategory cat = EventMediaCategory.builder()
                .community(community)
                .name(req.getName().trim())
                .sortOrder(order)
                .build();
        return toResponse(categoryRepo.save(cat));
    }

    @Transactional
    public void delete(Long id, Long communityId) {
        EventMediaCategory cat = categoryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        if (!cat.getCommunity().getId().equals(communityId)) {
            throw new IllegalStateException("Not authorized to delete this category");
        }
        categoryRepo.delete(cat);
    }

    private EventMediaCategoryResponse toResponse(EventMediaCategory c) {
        return EventMediaCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .sortOrder(c.getSortOrder())
                .build();
    }
}
