package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.CategoryRequest;
import com.manacommunity.api.marketplace.dto.CategoryResponse;
import com.manacommunity.api.marketplace.entity.MarketplaceCategory;
import com.manacommunity.api.marketplace.repository.MarketplaceCategoryRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final MarketplaceCategoryRepository categoryRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories(Long communityId) {
        List<MarketplaceCategory> global = categoryRepo.findByActiveOrderBySortOrderAsc(true)
                .stream()
                .filter(c -> c.getCommunity() == null)
                .toList();

        List<MarketplaceCategory> result = new ArrayList<>(global);

        if (communityId != null) {
            List<MarketplaceCategory> communitySpecific =
                    categoryRepo.findByCommunityIdAndActiveOrderBySortOrderAsc(communityId, true);
            result.addAll(communitySpecific);
        }

        return result.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return toResponse(categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id)));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req, Community community) {
        MarketplaceCategory.MarketplaceCategoryBuilder builder = MarketplaceCategory.builder()
                .name(req.getName())
                .slug(req.getName().toLowerCase().replaceAll("\\s+", "-"))
                .icon(req.getIcon())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .community(community);

        if (req.getParentId() != null) {
            MarketplaceCategory parent = categoryRepo.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", req.getParentId()));
            builder.parent(parent);
        }

        MarketplaceCategory saved = categoryRepo.save(builder.build());
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.MARKETPLACE,
                "MarketplaceCategory", String.valueOf(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        MarketplaceCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        category.setName(req.getName());
        category.setSlug(req.getName().toLowerCase().replaceAll("\\s+", "-"));
        category.setIcon(req.getIcon());
        if (req.getSortOrder() != null) {
            category.setSortOrder(req.getSortOrder());
        }

        if (req.getParentId() != null) {
            MarketplaceCategory parent = categoryRepo.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", req.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        MarketplaceCategory updated = categoryRepo.save(category);
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.MARKETPLACE,
                "MarketplaceCategory", String.valueOf(updated.getId()));
        return toResponse(updated);
    }

    @Transactional
    public void toggleActive(Long id) {
        MarketplaceCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setActive(!category.isActive());
        categoryRepo.save(category);
        auditService.record(AuditAction.CONFIG_UPDATED, AuditModule.MARKETPLACE,
                "MarketplaceCategory", String.valueOf(id),
                String.valueOf(!category.isActive()), String.valueOf(category.isActive()));
    }

    private CategoryResponse toResponse(MarketplaceCategory c) {
        return CategoryResponse.builder()
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
