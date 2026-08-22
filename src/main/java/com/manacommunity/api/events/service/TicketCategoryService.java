package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.TicketCategoryMaster;
import com.manacommunity.api.events.repository.TicketCategoryMasterRepository;
import com.manacommunity.api.exception.InvalidInputException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCategoryService {

    private final TicketCategoryMasterRepository masterRepo;

    private static final String[][] DEFAULT_CATEGORIES = {
            {"General", "Standard entry ticket for one person", "1"},
            {"VIP", "Premium access with priority seating", "2"},
            {"Family Pass", "Group admission for family members", "3"},
            {"Individual", "Single attendee pass", "4"},
            {"Couple", "Pass for two attendees", "5"},
            {"Kids / Student", "Discounted pass for children and students", "6"},
            {"Senior Citizen", "Special pass for senior residents", "7"},
            {"Sponsor", "Special pass for event donors and sponsors", "8"},
            {"Volunteer", "Event crew and volunteer pass", "9"},
            {"Early Bird", "Discounted early registration pass", "10"}
    };

    @Transactional
    public List<TicketCategoryMaster> getCategories(Long communityId) {
        List<TicketCategoryMaster> list = masterRepo.findActiveByCommunityIdOrGlobal(communityId);
        if (list.isEmpty()) {
            seedDefaults(communityId);
            list = masterRepo.findActiveByCommunityIdOrGlobal(communityId);
        }
        return list;
    }

    @Transactional
    public TicketCategoryMaster createCategory(Long communityId, String name, String description, Integer displayOrder) {
        if (name == null || name.trim().isBlank()) {
            throw new InvalidInputException("Category name must not be blank.");
        }
        String cleanName = name.trim();
        if (communityId != null && masterRepo.existsByCommunityIdAndNameIgnoreCase(communityId, cleanName)) {
            return masterRepo.findFirstByCommunityIdAndNameIgnoreCase(communityId, cleanName)
                    .orElseThrow(() -> new InvalidInputException("Category already exists."));
        }
        if (communityId == null && masterRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(cleanName)) {
            return masterRepo.findFirstByCommunityIdAndNameIgnoreCase(null, cleanName)
                    .orElseThrow(() -> new InvalidInputException("Category already exists."));
        }

        TicketCategoryMaster master = TicketCategoryMaster.builder()
                .communityId(communityId)
                .name(cleanName)
                .description(description != null ? description.trim() : null)
                .displayOrder(displayOrder != null ? displayOrder : 0)
                .isActive(true)
                .build();
        return masterRepo.save(master);
    }

    @Transactional
    public void deleteCategory(Long id) {
        masterRepo.findById(id).ifPresent(masterRepo::delete);
    }

    private void seedDefaults(Long communityId) {
        for (String[] def : DEFAULT_CATEGORIES) {
            String name = def[0];
            String desc = def[1];
            int order = Integer.parseInt(def[2]);
            if (!masterRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(name)
                    && (communityId == null || !masterRepo.existsByCommunityIdAndNameIgnoreCase(communityId, name))) {
                masterRepo.save(TicketCategoryMaster.builder()
                        .communityId(communityId)
                        .name(name)
                        .description(desc)
                        .displayOrder(order)
                        .isActive(true)
                        .build());
            }
        }
    }
}
