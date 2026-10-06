package com.manacommunity.api.emergency.controller;

import com.manacommunity.api.emergency.dto.SosResolveRequest;
import com.manacommunity.api.emergency.dto.SosTriggerRequest;
import com.manacommunity.api.emergency.service.SosIncidentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/api/v1/emergency")
@RequiredArgsConstructor
public class EmergencySosAliasController {

    private final SosIncidentService sosService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/sos/trigger")
    public ResponseEntity<?> triggerSos(
            @RequestBody SosTriggerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.triggerSos(user, request));
    }

    @PostMapping("/sos/{id}/resolve")
    public ResponseEntity<?> resolveSos(
            @PathVariable Long id,
            @RequestBody SosResolveRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.resolveSos(user, id, request));
    }

    @PostMapping("/sos/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeSos(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.acknowledgeSos(user, id));
    }

    @GetMapping("/sos/active")
    public ResponseEntity<?> activeIncidents(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.getActiveIncidents(user.getCommunity().getId()));
    }
}
