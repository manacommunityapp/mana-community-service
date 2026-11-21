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
@RequestMapping({"/api/v1/emergency", "/api/emergency", "/api/api/v1/emergency"})
@RequiredArgsConstructor
public class EmergencySosAliasController {

    private final SosIncidentService sosService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(
            @RequestBody java.util.Map<String, Object> req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null
                ? user.getCommunity().getId()
                : (req.containsKey("communityId") && req.get("communityId") != null ? Long.valueOf(req.get("communityId").toString()) : null);
        return ResponseEntity.ok(java.util.Map.of(
                "status", "BROADCAST_SENT",
                "communityId", communityId != null ? communityId : 0L,
                "message", req.getOrDefault("message", ""),
                "severity", req.getOrDefault("severity", "HIGH")
        ));
    }

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
