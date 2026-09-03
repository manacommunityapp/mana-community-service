package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCulturalCategory;
import com.manacommunity.api.events.repository.CulturalCategoryRepository;
import com.manacommunity.api.events.service.CulturalCategoryService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
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
            seedDefaultCategories();
            list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    // Separate writable transaction so seeds are committed even when called from a readOnly context.
    @Transactional
    public void seedDefaultCategories() {
        for (String defaultName : DEFAULT_CATEGORIES) {
            if (!repository.existsByNameIgnoreCase(defaultName)) {
                repository.save(new EventCulturalCategory(null, defaultName, "Default cultural category"));
            }
        }
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

    @Override
    public EventCulturalCategory updateCulturalCategory(Long id, Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Category name cannot be empty");
        }
        EventCulturalCategory existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cultural category", id));
        if (existing.getCommunityId() == null || !existing.getCommunityId().equals(communityId)) {
            throw new InvalidInputException("Cannot modify a default or another community's category");
        }
        existing.setName(name.trim());
        existing.setDescription(description);
        return repository.save(existing);
    }

    @Override
    public void deleteCulturalCategory(Long id, Long communityId) {
        EventCulturalCategory existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cultural category", id));
        if (existing.getCommunityId() == null || !existing.getCommunityId().equals(communityId)) {
            throw new InvalidInputException("Cannot delete a default or another community's category");
        }
        repository.delete(existing);
    }
}
