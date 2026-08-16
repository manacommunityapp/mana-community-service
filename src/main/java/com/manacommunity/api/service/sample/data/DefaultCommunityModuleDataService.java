package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.constants.ModuleConstants;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityModule;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCommunityModuleDataService {

    private final CommunityModuleRepository communityModuleRepository;
    private final CommunityRepository communityRepository;

    @Transactional
    public void seedDefaultModulesForCommunity(Long communityId) {
        seedModulesForCommunity(communityId, Set.of());
    }

    @Transactional
    public void seedModulesForCommunity(Long communityId, Set<String> enabledModuleKeys) {
        if (communityModuleRepository.existsByCommunityId(communityId)) {
            log.info("Modules already initialized for community ID: {}", communityId);
            return;
        }

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found with ID: " + communityId));

        LocalDateTime now = LocalDateTime.now();
        List<CommunityModule> modules = ModuleConstants.ALL_MODULES.stream()
                .map(def -> CommunityModule.builder()
                        .community(community)
                        .moduleKey(def.key())
                        .moduleLabel(def.label())
                        .isEnabled(enabledModuleKeys.contains(def.key()))
                        .sortOrder(def.sortOrder())
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        communityModuleRepository.saveAll(modules);
        log.info("Seeded modules for community '{}' (ID: {}), enabled: {}",
                community.getName(), communityId, enabledModuleKeys);
    }
}
