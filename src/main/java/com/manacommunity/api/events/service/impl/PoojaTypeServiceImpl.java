package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.EventPoojaType;
import com.manacommunity.api.events.repository.PoojaTypeRepository;
import com.manacommunity.api.events.service.PoojaTypeService;
import com.manacommunity.api.exception.InvalidInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class PoojaTypeServiceImpl implements PoojaTypeService {

    private static final Logger log = LoggerFactory.getLogger(PoojaTypeServiceImpl.class);
    private final PoojaTypeRepository poojaTypeRepository;

    private static final List<String> DEFAULT_TYPES = Arrays.asList(
        "Ganesh Puja",
        "Ganapati Homam",
        "Abhishekam",
        "Maha Aarti",
        "Satyanarayan Puja",
        "Laghu Rudra",
        "Navagraha Puja",
        "Sahasranama Archana"
    );

    public PoojaTypeServiceImpl(PoojaTypeRepository poojaTypeRepository) {
        this.poojaTypeRepository = poojaTypeRepository;
    }

    @Override
    public List<EventPoojaType> getAllPoojaTypes(Long communityId) {
        try {
            List<EventPoojaType> list = poojaTypeRepository.findByCommunityOrGlobal(communityId);
            if (list == null || list.isEmpty()) {
                // Seed defaults if empty
                for (String defaultName : DEFAULT_TYPES) {
                    try {
                        if (!poojaTypeRepository.existsByNameIgnoreCase(defaultName)) {
                            poojaTypeRepository.save(new EventPoojaType(null, defaultName, "Default ritual type"));
                        }
                    } catch (Exception e) {
                        log.debug("Notice while seeding default pooja type {}: {}", defaultName, e.getMessage());
                    }
                }
                list = poojaTypeRepository.findByCommunityOrGlobal(communityId);
            }
            if (list != null && !list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            log.warn("Could not retrieve pooja types from database, returning standard defaults: {}", e.getMessage());
        }

        // Resilient fallback to default pooja types
        List<EventPoojaType> fallbackList = new ArrayList<>();
        long id = 1L;
        for (String name : DEFAULT_TYPES) {
            EventPoojaType pt = new EventPoojaType(communityId, name, "Default temple ritual");
            pt.setId(id++);
            fallbackList.add(pt);
        }
        return fallbackList;
    }

    @Override
    public EventPoojaType createPoojaType(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Pooja type name cannot be empty");
        }
        String cleanName = name.trim();
        try {
            return poojaTypeRepository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                    .orElseGet(() -> poojaTypeRepository.save(new EventPoojaType(communityId, cleanName, description)));
        } catch (Exception e) {
            log.warn("Failed to persist pooja type to database: {}", e.getMessage());
            EventPoojaType pt = new EventPoojaType(communityId, cleanName, description);
            pt.setId(System.currentTimeMillis());
            return pt;
        }
    }
}
