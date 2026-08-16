package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CompetitionAgeGroup;
import com.manacommunity.api.events.repository.CompetitionAgeGroupRepository;
import com.manacommunity.api.events.service.CompetitionAgeGroupService;
import com.manacommunity.api.exception.InvalidInputException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class CompetitionAgeGroupServiceImpl implements CompetitionAgeGroupService {

    private final CompetitionAgeGroupRepository repository;

    private static final List<String> DEFAULT_AGE_GROUPS = Arrays.asList(
        "Kids (Under 12)", "Youth (12–25)", "Adults (25+)", "Seniors (60+)", "Open (All Ages)"
    );

    public CompetitionAgeGroupServiceImpl(CompetitionAgeGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetitionAgeGroup> getAllCompetitionAgeGroups(Long communityId) {
        List<CompetitionAgeGroup> list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            for (String defaultName : DEFAULT_AGE_GROUPS) {
                if (!repository.existsByNameIgnoreCase(defaultName)) {
                    repository.save(new CompetitionAgeGroup(null, defaultName, "Default competition age group"));
                }
            }
            list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    @Override
    public CompetitionAgeGroup createCompetitionAgeGroup(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Age group name cannot be empty");
        }
        String cleanName = name.trim();
        return repository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                .orElseGet(() -> repository.save(new CompetitionAgeGroup(communityId, cleanName, description)));
    }
}
