package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketCategoryRequest;
import com.manacommunity.api.marketplace.dto.MarketCategoryResponse;
import com.manacommunity.api.marketplace.entity.MarketListingCategory;
import com.manacommunity.api.marketplace.repository.MarketCategoryRepository;
import com.manacommunity.api.model.Community;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketCategoryService {

    private final MarketCategoryRepository categoryRepository;

    public List<MarketCategoryResponse> getAllActive() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MarketCategoryResponse> getSubCategories(Long parentId) {
        return categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(parentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketCategoryResponse create(MarketCategoryRequest req, Community community) {
        MarketListingCategory parent = null;
        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
        }

        String slug = req.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");

        MarketListingCategory cat = MarketListingCategory.builder()
                .name(req.getName())
                .slug(slug)
                .icon(req.getIcon())
                .parent(parent)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .active(true)
                .community(community)
                .build();

        return toResponse(categoryRepository.save(cat));
    }

    private MarketCategoryResponse toResponse(MarketListingCategory c) {
        return MarketCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .icon(c.getIcon())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .sortOrder(c.getSortOrder())
                .active(c.isActive())
                .build();
    }
}
