package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCulturalCategory;
import com.manacommunity.api.events.repository.CulturalCategoryRepository;
import com.manacommunity.api.events.service.CulturalCategoryService;
import com.manacommunity.api.exception.InvalidInputException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class CulturalCategoryServiceImpl implements CulturalCategoryService {

    private final CulturalCategoryRepository repository;

    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        "Classical Dance", "Folk Dance", "Fusion Dance", "Classical Music",
        "Film Music", "Devotional Music", "Drama / Skit", "Stand-up / Comedy",
        "Fashion Show", "Mime / Nritya", "Instrumental", "Choir"
    );

    public CulturalCategoryServiceImpl(CulturalCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventCulturalCategory> getAllCulturalCategories(Long communityId) {
        List<EventCulturalCategory> list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            for (String defaultName : DEFAULT_CATEGORIES) {
                if (!repository.existsByNameIgnoreCase(defaultName)) {
                    repository.save(new EventCulturalCategory(null, defaultName, "Default cultural category"));
                }
            }
            list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    @Override
    public EventCulturalCategory createCulturalCategory(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Category name cannot be empty");
        }
        String cleanName = name.trim();
        return repository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                .orElseGet(() -> repository.save(new EventCulturalCategory(communityId, cleanName, description)));
    }
}
