package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.BlockConfigResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.CommunityModule;
import com.manacommunity.api.repository.CommunityModuleRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.response.CommunityResponse;
import com.manacommunity.api.service.CommunityBlockConfigService;
import com.manacommunity.api.service.CommunityModuleService;
import com.manacommunity.api.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityServiceImpl implements CommunityService {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityRoleInitializer communityRoleInitializer;

    @Autowired
    private CommunityModuleService communityModuleService;

    @Autowired
    private CommunityModuleRepository communityModuleRepo;

    @Autowired
    private CommunityBlockConfigService blockConfigService;

    @Override
    public List<CommunityResponse> getAllCommunities() {
        return communityRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toPublicResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommunityResponse> getCommunitiesByType(String type) {
        return communityRepository.findByActiveTrueAndTypeIgnoreCaseOrderByNameAsc(type)
                .stream()
                .map(this::toPublicResponse)
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CommunityResponse createCommunity(CommunityResponse request) {
        Community community = toEntity(request);
        community.setActive(true);
        Community saved = communityRepository.save(community);
        communityRoleInitializer.initializeCommunityRoles(saved);
        communityModuleService.initializeModulesForCommunity(saved.getId());
        // Save block layout for apartment communities (custom if provided, otherwise default A/B/C/D)
        if ("APARTMENT".equalsIgnoreCase(saved.getType())) {
            if (request.getBlockConfigs() != null && !request.getBlockConfigs().isEmpty()) {
                blockConfigService.saveAllBlockConfigs(saved.getId(), request.getBlockConfigs());
            } else {
                blockConfigService.seedDefaultBlocks(saved.getId());
            }
        }
        return toResponse(saved);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CommunityResponse updateCommunity(Long id, CommunityResponse request) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("Community", id));
        community.setName(request.getName());
        community.setType(request.getType());
        community.setCity(request.getCity());
        community.setState(request.getState());
        community.setArea(request.getArea());
        community.setSubtype(request.getSubtype());
        community.setInviteCode(request.getInviteCode());
        Community saved = communityRepository.save(community);
        // Save / update block layout for apartment communities
        if ("APARTMENT".equalsIgnoreCase(saved.getType())) {
            if (request.getBlockConfigs() != null && !request.getBlockConfigs().isEmpty()) {
                blockConfigService.saveAllBlockConfigs(saved.getId(), request.getBlockConfigs());
            } else {
                blockConfigService.seedDefaultBlocks(saved.getId());
            }
        }
        return toResponse(saved);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CommunityResponse updateEnabledModules(Long id, List<String> modules) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("Community", id));

        var allModules = communityModuleRepo.findByCommunityIdOrderBySortOrderAsc(id);
        for (CommunityModule cm : allModules) {
            boolean shouldEnable = modules != null && modules.contains(cm.getModuleKey());
            cm.setIsEnabled(shouldEnable);
        }
        communityModuleRepo.saveAll(allModules);

        return toResponse(community);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteCommunity(Long id) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("Community", id));
        community.setActive(false);
        communityRepository.save(community);
    }

    @Autowired
    private com.manacommunity.api.user.repository.AppUserRepository appUserRepo;

    @Override
    public boolean checkUnitExists(Long communityId, String inviteCode, String block, String flatNo) {
        if (block == null || flatNo == null || block.isBlank() || flatNo.isBlank()) {
            return false;
        }
        Long targetCommunityId = communityId;
        if (targetCommunityId == null && inviteCode != null && !inviteCode.isBlank()) {
            targetCommunityId = communityRepository.findByInviteCode(inviteCode)
                    .map(Community::getId)
                    .orElse(null);
        }
        if (targetCommunityId == null) {
            return false;
        }
        return appUserRepo.existsByCommunityIdAndBlockIgnoreCaseAndFlatNoIgnoreCase(
                targetCommunityId, block.trim(), flatNo.trim());
    }

    private Community toEntity(CommunityResponse r) {
        return Community.builder()
                .name(r.getName())
                .type(r.getType())
                .city(r.getCity())
                .state(r.getState())
                .area(r.getArea())
                .subtype(r.getSubtype())
                .inviteCode(r.getInviteCode())
                .build();
    }

    private CommunityResponse toPublicResponse(Community c) {
        List<String> modules = communityModuleService.getEnabledModuleKeys(c.getId());
        List<BlockConfigResponse> blocks = resolveBlockConfigs(c);
        CommunityResponse resp = new CommunityResponse();
        resp.setId(c.getId());
        resp.setName(c.getName());
        resp.setType(c.getType());
        resp.setCity(c.getCity());
        resp.setState(c.getState());
        resp.setArea(c.getArea());
        resp.setSubtype(c.getSubtype());
        resp.setInviteCode(c.getInviteCode());
        resp.setActive(c.getActive());
        resp.setEnabledModules(modules);
        resp.setBlockConfigs(blocks);
        return resp;
    }

    private CommunityResponse toResponse(Community c) {
        List<String> modules = communityModuleService.getEnabledModuleKeys(c.getId());
        List<BlockConfigResponse> blocks = resolveBlockConfigs(c);
        CommunityResponse resp = new CommunityResponse();
        resp.setId(c.getId());
        resp.setName(c.getName());
        resp.setType(c.getType());
        resp.setCity(c.getCity());
        resp.setState(c.getState());
        resp.setArea(c.getArea());
        resp.setSubtype(c.getSubtype());
        resp.setInviteCode(c.getInviteCode());
        resp.setActive(c.getActive());
        resp.setEnabledModules(modules);
        resp.setBlockConfigs(blocks);
        return resp;
    }

    /**
     * Returns block configs for APARTMENT communities; null for other types
     * so the field is omitted from the JSON response.
     */
    private List<BlockConfigResponse> resolveBlockConfigs(Community c) {
        if (!"APARTMENT".equalsIgnoreCase(c.getType())) {
            return null;
        }
        List<BlockConfigResponse> configs = blockConfigService.getBlockConfigs(c.getId());
        return configs.isEmpty() ? null : configs;
    }
}
