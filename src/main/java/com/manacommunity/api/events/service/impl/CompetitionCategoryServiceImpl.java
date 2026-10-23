package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCompetitionCategory;
import com.manacommunity.api.events.repository.CompetitionCategoryRepository;
import com.manacommunity.api.events.service.CompetitionCategoryService;
import com.manacommunity.api.exception.InvalidInputException;
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
    public List<EventCompetitionCategory> getAllCompetitionCategories(Long communityId) {
        List<EventCompetitionCategory> list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            for (String defaultName : DEFAULT_CATEGORIES) {
                if (!repository.existsByNameIgnoreCase(defaultName)) {
                    repository.save(new EventCompetitionCategory(null, defaultName, "Default competition category"));
                }
            }
            list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    @Override
    public EventCompetitionCategory createCompetitionCategory(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("EventCompetition category name cannot be empty");
        }
        String cleanName = name.trim();
        return repository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                .orElseGet(() -> repository.save(new EventCompetitionCategory(communityId, cleanName, description)));
    }
}
