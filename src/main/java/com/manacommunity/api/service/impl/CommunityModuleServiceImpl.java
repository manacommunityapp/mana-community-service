package com.manacommunity.api.service.impl;

import com.manacommunity.api.constants.ModuleConstants;
import com.manacommunity.api.dto.CommunityModuleBulkRequest;
import com.manacommunity.api.dto.CommunityModuleResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityModule;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.service.CommunityModuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityModuleServiceImpl implements CommunityModuleService {

    private final CommunityModuleRepository communityModuleRepo;
    private final CommunityRepository communityRepo;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private static final long MODULES_CACHE_TTL_MS = 300_000L; // 5 minutes
    private record CachedModules(long timestamp, List<String> modules) {}
    private final java.util.concurrent.ConcurrentHashMap<Long, CachedModules> enabledModulesCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public List<CommunityModuleResponse> getModulesForCommunity(Long communityId) {
        return communityModuleRepo.findByCommunityIdOrderBySortOrderAsc(communityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<String> getEnabledModuleKeys(Long communityId) {
        if (communityId == null) {
            return java.util.Collections.emptyList();
        }
        CachedModules cached = enabledModulesCache.get(communityId);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.timestamp()) < MODULES_CACHE_TTL_MS) {
            return cached.modules();
        }

        if (!communityModuleRepo.existsByCommunityId(communityId)) {
            initializeModulesForCommunity(communityId);
        }
        List<String> result = communityModuleRepo.findEnabledByCommunityId(communityId).stream()
                .map(CommunityModule::getModuleKey)
                .toList();
        enabledModulesCache.put(communityId, new CachedModules(now, result));
        return result;
    }

    @Override
    @Transactional
    public CommunityModuleResponse toggleModule(Long communityId, String moduleKey, boolean enabled) {
        enabledModulesCache.remove(communityId);
        evictRedisCache(communityId);
        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        CommunityModule cm = communityModuleRepo.findByCommunityIdAndModuleKey(communityId, moduleKey)
                .orElseGet(() -> {
                    ModuleConstants.ModuleDef def = resolveModuleDef(moduleKey);
                    LocalDateTime now = LocalDateTime.now();
                    return CommunityModule.builder()
                            .community(community)
                            .moduleKey(def.key())
                            .moduleLabel(def.label())
                            .isEnabled(false)
                            .sortOrder(def.sortOrder())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                });

        cm.setIsEnabled(enabled);
        return toResponse(communityModuleRepo.save(cm));
    }

    @Override
    @Transactional
    public List<CommunityModuleResponse> bulkUpdate(CommunityModuleBulkRequest request) {
        Community community = communityRepo.findById(request.communityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", request.communityId()));

        List<CommunityModuleResponse> results = request.modules().stream().map(toggle -> {
            CommunityModule cm = communityModuleRepo
                    .findByCommunityIdAndModuleKey(request.communityId(), toggle.moduleKey())
                    .orElseGet(() -> {
                        ModuleConstants.ModuleDef def = resolveModuleDef(toggle.moduleKey());
                        LocalDateTime now = LocalDateTime.now();
                        return CommunityModule.builder()
                                .community(community)
                                .moduleKey(def.key())
                                .moduleLabel(def.label())
                                .isEnabled(false)
                                .sortOrder(def.sortOrder())
                                .createdAt(now)
                                .updatedAt(now)
                                .build();
                    });
            cm.setIsEnabled(toggle.isEnabled());
            return toResponse(communityModuleRepo.save(cm));
        }).toList();

        log.info("Bulk-updated {} modules for community {}", results.size(), request.communityId());
        enabledModulesCache.remove(request.communityId());
        evictRedisCache(request.communityId());
        return results;
    }

    @Override
    @Transactional
    public void initializeModulesForCommunity(Long communityId) {
        if (communityModuleRepo.existsByCommunityId(communityId)) {
            return;
        }

        Community community = communityRepo.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

        LocalDateTime now = LocalDateTime.now();
        List<CommunityModule> modules = ModuleConstants.ALL_MODULES.stream()
                .map(def -> CommunityModule.builder()
                        .community(community)
                        .moduleKey(def.key())
                        .moduleLabel(def.label())
                        .isEnabled(false)
                        .sortOrder(def.sortOrder())
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        communityModuleRepo.saveAll(modules);
        log.info("Initialized {} modules for community '{}'", modules.size(), community.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleEnabled(Long communityId, String moduleKey) {
        if (!communityModuleRepo.existsByCommunityId(communityId)) {
            return false;
        }
        return communityModuleRepo.isModuleEnabled(communityId, moduleKey);
    }

    private ModuleConstants.ModuleDef resolveModuleDef(String moduleKey) {
        return ModuleConstants.ALL_MODULES.stream()
                .filter(m -> m.key().equals(moduleKey))
                .findFirst()
                .orElseGet(() -> new ModuleConstants.ModuleDef(moduleKey, formatLabel(moduleKey), 99));
    }

    private String formatLabel(String moduleKey) {
        if (moduleKey == null || moduleKey.isBlank()) {
            return "Unknown Module";
        }
        String[] words = moduleKey.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private void evictRedisCache(Long communityId) {
        if (redisTemplate != null && communityId != null) {
            try {
                redisTemplate.delete("community:modules:" + communityId);
            } catch (Exception e) {
                log.debug("Failed to evict Redis cache for community {}: {}", communityId, e.getMessage());
            }
        }
    }

    private CommunityModuleResponse toResponse(CommunityModule cm) {
        return new CommunityModuleResponse(
                cm.getId(),
                cm.getCommunity().getId(),
                cm.getModuleKey(),
                cm.getModuleLabel(),
                cm.getIsEnabled(),
                cm.getSortOrder()
        );
    }
}
