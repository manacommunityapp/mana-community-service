package com.manacommunity.api.mw.controller;

import com.manacommunity.api.mw.dto.UpdateIntegrationConfigRequest;
import com.manacommunity.api.mw.entity.MwIntegrationConfig;
import com.manacommunity.api.mw.service.MwIntegrationConfigService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mw/integrations")
@RequiredArgsConstructor
public class MwIntegrationController {

    private final MwIntegrationConfigService configService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{orgId}/config")
    public ResponseEntity<MwIntegrationConfig> getConfig(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(configService.getOrCreateConfig(orgId));
    }

    @PutMapping("/{orgId}/config")
    public ResponseEntity<MwIntegrationConfig> updateConfig(
            @PathVariable Long orgId,
            @RequestBody UpdateIntegrationConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(configService.updateConfig(orgId, req));
    }
}
