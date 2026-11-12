package com.manacommunity.api.mw.controller;

import com.manacommunity.api.hrm.service.HrEmployeeService;
import com.manacommunity.api.mw.dto.UpdateIntegrationConfigRequest;
import com.manacommunity.api.mw.entity.MwIntegrationConfig;
import com.manacommunity.api.mw.service.MwIntegrationConfigService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mw/integrations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MwIntegrationController {

    private final MwIntegrationConfigService configService;
    private final LoggedInUserService loggedInUserService;
    private final HrEmployeeService employeeService;

    @GetMapping("/{orgId}/config")
    public ResponseEntity<MwIntegrationConfig> getConfig(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(configService.getOrCreateConfig(orgId));
    }

    @PutMapping("/{orgId}/config")
    public ResponseEntity<MwIntegrationConfig> updateConfig(
            @PathVariable Long orgId,
            @RequestBody UpdateIntegrationConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        employeeService.resolveAndVerifyMembership(orgId, user.getId());
        return ResponseEntity.ok(configService.updateConfig(orgId, req));
    }
}
