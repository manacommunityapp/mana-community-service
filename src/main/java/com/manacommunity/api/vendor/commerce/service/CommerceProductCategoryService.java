package com.manacommunity.api.vendor.commerce.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.vendor.commerce.dto.CommerceProductCategoryRequest;
import com.manacommunity.api.vendor.commerce.dto.CommerceProductCategoryResponse;
import com.manacommunity.api.vendor.commerce.model.CommerceProductCategory;
import com.manacommunity.api.vendor.commerce.repository.CommerceProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommerceProductCategoryService {

    private final CommerceProductCategoryRepository categoryRepo;
    private final CommunityRepository communityRepo;

    private static final Pattern NON_LATIN   = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE  = Pattern.compile("[\\s]+");

    @Transactional(readOnly = true)
    public List<CommerceProductCategoryResponse> getRootCategories(Long communityId) {
        return categoryRepo.findRootCategoriesByCommunity(communityId).stream()
                .map(c -> toResponse(c, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommerceProductCategoryResponse> getAllCategories(Long communityId) {
        return categoryRepo.findByCommunityIdAndActiveTrueOrderByDisplayOrderAsc(communityId).stream()
                .map(c -> toResponse(c, false))
                .toList();
    }

    @Transactional
    public CommerceProductCategoryResponse create(CommerceProductCategoryRequest req, Long communityId) {
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        String slug = buildSlug(req.getSlug() != null ? req.getSlug() : req.getName(), communityId);

        CommerceProductCategory parent = null;
        if (req.getParentId() != null) {
            parent = categoryRepo.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getParentId()));
        }

        CommerceProductCategory category = CommerceProductCategory.builder()
                .name(req.getName())
                .slug(slug)
                .icon(req.getIcon())
                .description(req.getDescription())
                .parent(parent)
                .community(community)
                .active(req.getActive() != null ? req.getActive() : true)
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .build();

        return toResponse(categoryRepo.save(category), false);
    }

    @Transactional
    public CommerceProductCategoryResponse update(Long id, CommerceProductCategoryRequest req, Long communityId) {
        CommerceProductCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        category.setName(req.getName());
        if (req.getIcon() != null)        category.setIcon(req.getIcon());
        if (req.getDescription() != null) category.setDescription(req.getDescription());
        if (req.getActive() != null)      category.setActive(req.getActive());
        if (req.getDisplayOrder() != null) category.setDisplayOrder(req.getDisplayOrder());

        if (req.getParentId() != null) {
            CommerceProductCategory parent = categoryRepo.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.getParentId()));
            category.setParent(parent);
        }

        return toResponse(categoryRepo.save(category), false);
    }

    @Transactional
    public void delete(Long id) {
        CommerceProductCategory category = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setActive(false);
        categoryRepo.save(category);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildSlug(String name, Long communityId) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD);
        base = WHITESPACE.matcher(base).replaceAll("-");
        base = NON_LATIN.matcher(base).replaceAll("");
        base = base.toLowerCase(Locale.ENGLISH);

        String candidate = base;
        int attempt = 0;
        while (categoryRepo.existsBySlugAndCommunityId(candidate, communityId)) {
            candidate = base + "-" + (++attempt);
        }
        return candidate;
    }

    private CommerceProductCategoryResponse toResponse(CommerceProductCategory c, boolean includeChildren) {
        return CommerceProductCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .icon(c.getIcon())
                .description(c.getDescription())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .active(c.getActive())
                .displayOrder(c.getDisplayOrder())
                .children(includeChildren && c.getChildren() != null
                        ? c.getChildren().stream()
                                .filter(ch -> Boolean.TRUE.equals(ch.getActive()))
                                .map(ch -> toResponse(ch, false))
                                .toList()
                        : null)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
