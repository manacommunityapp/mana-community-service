package com.manacommunity.api.events.service.impl;

import com.manacommunity.api.events.entity.PoojaType;
import com.manacommunity.api.events.repository.PoojaTypeRepository;
import com.manacommunity.api.events.service.PoojaTypeService;
import com.manacommunity.api.exception.InvalidInputException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class PoojaTypeServiceImpl implements PoojaTypeService {

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
    @Transactional(readOnly = true)
    public List<PoojaType> getAllPoojaTypes(Long communityId) {
        List<PoojaType> list = poojaTypeRepository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        if (list.isEmpty()) {
            // Seed defaults if empty
            for (String defaultName : DEFAULT_TYPES) {
                if (!poojaTypeRepository.existsByNameIgnoreCase(defaultName)) {
                    poojaTypeRepository.save(new PoojaType(null, defaultName, "Default ritual type"));
                }
            }
            list = poojaTypeRepository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId);
        }
        return list;
    }

    @Override
    public PoojaType createPoojaType(Long communityId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidInputException("Pooja type name cannot be empty");
        }
        String cleanName = name.trim();
        return poojaTypeRepository.findByNameIgnoreCaseAndCommunityId(cleanName, communityId)
                .orElseGet(() -> poojaTypeRepository.save(new PoojaType(communityId, cleanName, description)));
    }
}
