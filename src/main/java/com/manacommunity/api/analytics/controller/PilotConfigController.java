package com.manacommunity.api.analytics.controller;

import com.manacommunity.api.analytics.dto.PilotConfigRequest;
import com.manacommunity.api.analytics.dto.PilotConfigResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.PilotCommunityConfig;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.PilotCommunityConfigRepository;
import com.manacommunity.api.service.CommunityModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PilotConfigController {

    private final PilotCommunityConfigRepository pilotConfigRepo;
    private final CommunityRepository communityRepository;
    private final CommunityModuleService communityModuleService;

    @GetMapping({"/api/v1/communities/{communityId}/pilot-config", "/api/communities/{communityId}/pilot-config"})
    public ResponseEntity<PilotConfigResponse> getPilotConfig(@PathVariable Long communityId) {
        PilotCommunityConfig config = pilotConfigRepo.findById(communityId).orElse(null);
        List<String> enabledModules = communityModuleService.getEnabledModuleKeys(communityId);

        return ResponseEntity.ok(PilotConfigResponse.builder()
                .pilot(config != null && Boolean.TRUE.equals(config.getIsPilot()))
                .cohort(config != null ? config.getCohort() : null)
                .enabledModules(enabledModules)
                .build());
    }

    @PutMapping({"/api/v1/communities/{communityId}/pilot-config", "/api/communities/{communityId}/pilot-config"})
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PilotConfigResponse> updatePilotConfig(
            @PathVariable Long communityId,
            @RequestBody PilotConfigRequest request) {

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));

        PilotCommunityConfig config = pilotConfigRepo.findById(communityId)
                .orElseGet(() -> PilotCommunityConfig.builder()
                        .community(community)
                        .communityId(communityId)
                        .build());

        config.setIsPilot(request.isPilot());
        config.setCohort(request.getCohort());
        pilotConfigRepo.save(config);

        List<String> enabledModules = communityModuleService.getEnabledModuleKeys(communityId);

        return ResponseEntity.ok(PilotConfigResponse.builder()
                .pilot(config.getIsPilot())
                .cohort(config.getCohort())
                .enabledModules(enabledModules)
                .build());
    }
}
