package com.manacommunity.api.emergency.controller;

import com.manacommunity.api.emergency.dto.*;
import com.manacommunity.api.emergency.entity.GateLockdownLog;
import com.manacommunity.api.emergency.service.SosIncidentService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class SosIncidentController {

    private final SosIncidentService sosService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/sos/trigger")
    public ResponseEntity<SosIncidentResponse> triggerSos(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SosTriggerRequest request
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.triggerSos(user, request));
    }

    @PostMapping("/sos/{id}/acknowledge")
    public ResponseEntity<SosIncidentResponse> acknowledgeSos(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.acknowledgeSos(user, id));
    }

    @PostMapping("/sos/{id}/dispatch")
    public ResponseEntity<SosIncidentResponse> dispatchGuard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String notes
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.dispatchGuard(id, user, notes));
    }

    @PostMapping("/sos/{id}/arrived")
    public ResponseEntity<SosIncidentResponse> recordArrival(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String notes
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.recordGuardArrival(user, id, notes));
    }

    @PostMapping("/sos/{id}/resolve")
    public ResponseEntity<SosIncidentResponse> resolveSos(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody SosResolveRequest request
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.resolveSos(user, id, request));
    }

    @GetMapping("/sos/active")
    public ResponseEntity<List<SosIncidentResponse>> getActiveIncidents(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.getActiveIncidents(user.getCommunity().getId()));
    }

    @GetMapping("/sos/my")
    public ResponseEntity<List<SosIncidentResponse>> getMyIncidents(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.getMyIncidents(user.getId()));
    }

    @GetMapping("/sos/history")
    public ResponseEntity<Page<SosIncidentResponse>> getIncidentHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.getIncidentHistory(user.getCommunity().getId(), page, size));
    }

    @PostMapping("/lockdown")
    public ResponseEntity<GateLockdownLog> executeGateLockdown(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody GateLockdownRequest request
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sosService.executeGateLockdown(user, request));
    }
}