package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.CulturalPerformanceType;
import com.manacommunity.api.events.repository.CulturalPerformanceTypeRepository;
import com.manacommunity.api.events.service.CulturalPerformanceTypeService;
import com.manacommunity.api.exception.InvalidInputException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class CulturalPerformanceTypeServiceImpl implements CulturalPerformanceTypeService {

    private final CulturalPerformanceTypeRepository repository;

    private static final List<String> DEFAULT_TYPES = Arrays.asList(
        "Solo", "Duo", "Trio", "Small Group (4–8)", "Large Group (9+)"
    );

    public CulturalPerformanceTypeServiceImpl(CulturalPerformanceTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CulturalPerformanceType> getAllPerformanceTypes(Long communityId) {
        List<CulturalPerformanceType> list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            for (String defaultName : DEFAULT_TYPES) {
                if (!repository.existsByNameIgnoreCase(defaultName)) {
                    repository.save(new CulturalPerformanceType(null, defaultName, "Default performance group type"));
                }
            }
            list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    @Override
    public CulturalPerformanceType createPerformanceType(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Performance type name cannot be empty");
        }
        String cleanName = name.trim();
        return repository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                .orElseGet(() -> repository.save(new CulturalPerformanceType(communityId, cleanName, description)));
    }
}
