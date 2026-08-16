package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CompetitionCategory;
import com.manacommunity.api.events.repository.CompetitionCategoryRepository;
import com.manacommunity.api.events.service.CompetitionCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class CompetitionCategoryServiceImpl implements CompetitionCategoryService {

    private final CompetitionCategoryRepository repository;

    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        "Rangoli", "Drawing / Painting", "Kolam", "Quiz", "Fancy Dress",
        "Classical Dance", "Folk Dance", "Singing", "Instrumental",
        "Essay / Elocution", "Sports / Games", "Cooking", "Flower Decoration"
    );

    public CompetitionCategoryServiceImpl(CompetitionCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetitionCategory> getAllCompetitionCategories(Long communityId) {
        List<CompetitionCategory> list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            for (String defaultName : DEFAULT_CATEGORIES) {
                if (!repository.existsByNameIgnoreCase(defaultName)) {
                    repository.save(new CompetitionCategory(null, defaultName, "Default competition category"));
                }
            }
            list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    @Override
    public CompetitionCategory createCompetitionCategory(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Competition category name cannot be empty");
        }
        String cleanName = name.trim();
        return repository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                .orElseGet(() -> repository.save(new CompetitionCategory(communityId, cleanName, description)));
    }
}
