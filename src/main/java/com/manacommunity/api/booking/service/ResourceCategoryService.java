package com.manacommunity.api.booking.service;

import com.manacommunity.api.booking.dto.ResourceCategoryRequest;
import com.manacommunity.api.booking.dto.ResourceCategoryResponse;
import com.manacommunity.api.booking.entity.ResourceCategory;
import com.manacommunity.api.booking.repository.ResourceCategoryRepository;
import com.manacommunity.api.booking.repository.ResourceRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceCategoryService {

    private final ResourceCategoryRepository categoryRepo;
    private final ResourceRepository resourceRepo;

    @Transactional(readOnly = true)
    public List<ResourceCategoryResponse> getCategories(Long communityId) {
        return categoryRepo.findByCommunityIdOrderByDisplayOrderAsc(communityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceCategoryResponse getCategory(Long id) {
        ResourceCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource category not found: " + id));
        return toResponse(category);
    }

    @Transactional
    public ResourceCategoryResponse createCategory(ResourceCategoryRequest req, Community community, AppUser user) {
        ResourceCategory category = ResourceCategory.builder()
                .name(req.getName())
                .icon(req.getIcon())
                .color(req.getColor())
                .description(req.getDescription())
                .displayOrder(req.getDisplayOrder())
                .imageUrl(req.getImageUrl())
                .community(community)
                .createdBy(user)
                .build();

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            category.setStatus(parseEnum(ResourceCategory.CategoryStatus.class, req.getStatus()));
        }

        return toResponse(categoryRepo.save(category));
    }

    @Transactional
    public ResourceCategoryResponse updateCategory(Long id, ResourceCategoryRequest req) {
        ResourceCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource category not found: " + id));

        if (req.getName() != null) category.setName(req.getName());
        if (req.getIcon() != null) category.setIcon(req.getIcon());
        if (req.getColor() != null) category.setColor(req.getColor());
        if (req.getDescription() != null) category.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) category.setDisplayOrder(req.getDisplayOrder());
        if (req.getImageUrl() != null) category.setImageUrl(req.getImageUrl());
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            category.setStatus(parseEnum(ResourceCategory.CategoryStatus.class, req.getStatus()));
        }

        return toResponse(categoryRepo.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        ResourceCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource category not found: " + id));
        category.setStatus(ResourceCategory.CategoryStatus.INACTIVE);
        categoryRepo.save(category);
    }

    private ResourceCategoryResponse toResponse(ResourceCategory c) {
        long resourceCount = resourceRepo.countByCategoryIdAndDeletedFalse(c.getId());
        return ResourceCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .icon(c.getIcon())
                .color(c.getColor())
                .description(c.getDescription())
                .displayOrder(c.getDisplayOrder())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .imageUrl(c.getImageUrl())
                .resourceCount(resourceCount)
                .communityId(c.getCommunity() != null ? c.getCommunity().getId() : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for " + enumClass.getSimpleName());
        }
    }
}
