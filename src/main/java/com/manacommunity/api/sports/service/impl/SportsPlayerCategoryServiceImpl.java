package com.manacommunity.api.sports.service.impl;
import com.manacommunity.api.model.Community;


import com.manacommunity.api.sports.dto.SportsPlayerCategoryRequest;
import com.manacommunity.api.exception.DuplicateResourceException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.sports.model.SportsPlayerCategory;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.sports.repository.SportsPlayerCategoryRepository;
import com.manacommunity.api.sports.service.SportsPlayerCategoryService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static com.manacommunity.api.constants.PermissionConstants.ROLE_SUPER_ADMIN;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsPlayerCategoryServiceImpl implements SportsPlayerCategoryService {

    private final SportsPlayerCategoryRepository categoryRepo;
    private final CommunityRepository communityRepo;

    @Override
    @Transactional(readOnly = true)
    public List<SportsPlayerCategory> getCategories(AppUser user) {
        if (user.hasRole(ROLE_SUPER_ADMIN)) {
            return categoryRepo.findAll();
        }
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId != null) {
            return categoryRepo.findDefaultAndCommunityCategories(communityId);
        }
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public SportsPlayerCategory createCategory(SportsPlayerCategoryRequest req) {
        String trimmedName = req.getName() != null ? req.getName().trim() : "";
        req.setName(trimmedName);
        if (categoryRepo.existsDuplicateName(trimmedName, req.getCommunityId(), null)) {
            throw new DuplicateResourceException("PlayerCategory", "name", trimmedName);
        }

        log.info("Creating player category name={} type={} communityId={}", req.getName(), req.getCategoryType(), req.getCommunityId());
        SportsPlayerCategory cat = SportsPlayerCategory.builder()
                .name(req.getName())
                .category_type(req.getCategoryType())
                .description(req.getDescription())
                .minAge(req.getMinAge())
                .maxAge(req.getMaxAge())
                .gender(req.getGender())
                .type(req.getType() != null ? req.getType() : "DEFAULT")
                .build();
        if (req.getCommunityId() != null) {
            cat.setCommunity(communityRepo.findById(req.getCommunityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId())));
        }
        return categoryRepo.save(cat);
    }

    @Override
    @Transactional
    public SportsPlayerCategory updateCategory(Long id, SportsPlayerCategoryRequest req) {
        SportsPlayerCategory cat = categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SportsPlayerCategory", id));

        String trimmedName = req.getName() != null ? req.getName().trim() : "";
        req.setName(trimmedName);
        Long commId = req.getCommunityId() != null ? req.getCommunityId()
                : (cat.getCommunity() != null ? cat.getCommunity().getId() : null);

        if (categoryRepo.existsDuplicateName(trimmedName, commId, id)) {
            throw new DuplicateResourceException("PlayerCategory", "name", trimmedName);
        }

        log.info("Updating player category id={} name={}", id, req.getName());
        cat.setName(req.getName());
        cat.setCategory_type(req.getCategoryType());
        cat.setDescription(req.getDescription());
        cat.setMinAge(req.getMinAge());
        cat.setMaxAge(req.getMaxAge());
        cat.setGender(req.getGender());
        if (req.getType() != null) {
            cat.setType(req.getType());
        }
        if (req.getCommunityId() != null) {
            cat.setCommunity(communityRepo.findById(req.getCommunityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId())));
        } else if ("DEFAULT".equalsIgnoreCase(req.getType())) {
            cat.setCommunity(null);
        }
        return categoryRepo.save(cat);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting player category id={}", id);
        categoryRepo.deleteById(id);
    }
}
