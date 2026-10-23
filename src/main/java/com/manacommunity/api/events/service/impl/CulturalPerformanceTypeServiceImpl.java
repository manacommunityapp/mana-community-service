package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventCulturalPerformanceType;
import com.manacommunity.api.events.repository.CulturalPerformanceTypeRepository;
import com.manacommunity.api.events.service.CulturalPerformanceTypeService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
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
    public List<EventCulturalPerformanceType> getAllPerformanceTypes(Long communityId) {
        List<EventCulturalPerformanceType> list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            seedDefaultTypes();
            list = repository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    // Separate writable transaction so seeds are committed even when called from a readOnly context.
    @Transactional
    public void seedDefaultTypes() {
        for (String defaultName : DEFAULT_TYPES) {
            if (!repository.existsByNameIgnoreCase(defaultName)) {
                repository.save(new EventCulturalPerformanceType(null, defaultName, "Default performance group type"));
            }
        }
    }

    @Override
    public EventCulturalPerformanceType createPerformanceType(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Performance type name cannot be empty");
        }
        String cleanName = name.trim();
        return repository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                .orElseGet(() -> repository.save(new EventCulturalPerformanceType(communityId, cleanName, description)));
    }

    @Override
    public EventCulturalPerformanceType updatePerformanceType(Long id, Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Performance type name cannot be empty");
        }
        EventCulturalPerformanceType existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance type", id));
        if (existing.getCommunityId() == null || !existing.getCommunityId().equals(communityId)) {
            throw new InvalidInputException("Cannot modify a default or another community's performance type");
        }
        existing.setName(name.trim());
        existing.setDescription(description);
        return repository.save(existing);
    }

    @Override
    public void deletePerformanceType(Long id, Long communityId) {
        EventCulturalPerformanceType existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Performance type", id));
        if (existing.getCommunityId() == null || !existing.getCommunityId().equals(communityId)) {
            throw new InvalidInputException("Cannot delete a default or another community's performance type");
        }
        repository.delete(existing);
    }
}
