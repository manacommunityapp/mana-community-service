package com.manacommunity.api.vendor.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.vendor.dto.VendorCategoryRequest;
import com.manacommunity.api.vendor.dto.VendorCategoryResponse;
import com.manacommunity.api.vendor.entity.VmsVendorCategory;
import com.manacommunity.api.vendor.repository.VmsVendorCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VmsVendorCategoryService {

    private final VmsVendorCategoryRepository categoryRepo;

    @Transactional(readOnly = true)
    public List<VendorCategoryResponse> getCategories(Long communityId) {
        List<VmsVendorCategory> roots = categoryRepo.findByCommunityIdAndParentIdIsNullAndIsActiveTrue(communityId);
        return roots.stream().map(this::toResponseWithChildren).toList();
    }

    @Transactional(readOnly = true)
    public List<VendorCategoryResponse> getAllCategories(Long communityId) {
        return categoryRepo.findByCommunityIdAndIsActiveTrue(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public VendorCategoryResponse create(VendorCategoryRequest req, Community community) {
        VmsVendorCategory category = VmsVendorCategory.builder()
                .name(req.getName())
                .description(req.getDescription())
                .icon(req.getIcon())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .community(community)
                .build();
        if (req.getParentId() != null) {
            VmsVendorCategory parent = categoryRepo.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getParentId()));
            category.setParent(parent);
        }
        return toResponse(categoryRepo.save(category));
    }

    @Transactional
    public VendorCategoryResponse update(Long id, VendorCategoryRequest req) {
        VmsVendorCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setName(req.getName());
        category.setDescription(req.getDescription());
        category.setIcon(req.getIcon());
        if (req.getSortOrder() != null) category.setSortOrder(req.getSortOrder());
        return toResponse(categoryRepo.save(category));
    }

    @Transactional
    public void delete(Long id) {
        VmsVendorCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setIsActive(false);
        categoryRepo.save(category);
    }

    private VendorCategoryResponse toResponse(VmsVendorCategory c) {
        return VendorCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .icon(c.getIcon())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .sortOrder(c.getSortOrder())
                .isActive(c.getIsActive())
                .build();
    }

    private VendorCategoryResponse toResponseWithChildren(VmsVendorCategory c) {
        List<VmsVendorCategory> children = categoryRepo.findByParentIdAndIsActiveTrue(c.getId());
        return VendorCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .icon(c.getIcon())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .sortOrder(c.getSortOrder())
                .isActive(c.getIsActive())
                .children(children.stream().map(this::toResponse).toList())
                .build();
    }
}
