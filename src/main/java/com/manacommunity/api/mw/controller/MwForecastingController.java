package com.manacommunity.api.mw.controller;

import com.manacommunity.api.mw.dto.CreateSnapshotRequest;
import com.manacommunity.api.mw.dto.UpdateForecastConfigRequest;
import com.manacommunity.api.mw.service.MwForecastingService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mw/forecasting")
@RequiredArgsConstructor
public class MwForecastingController {

    private final MwForecastingService forecastingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{orgId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSummary(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(forecastingService.getForecastSummary(orgId));
    }

    @GetMapping("/{orgId}/config")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getConfig(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(forecastingService.getOrCreateConfig(orgId));
    }

    @PutMapping("/{orgId}/config")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateConfig(
            @PathVariable Long orgId,
            @RequestBody UpdateForecastConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(forecastingService.updateConfig(orgId, req));
    }

    @GetMapping("/{orgId}/snapshots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSnapshots(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(forecastingService.getSnapshots(orgId));
    }

    @PostMapping("/{orgId}/snapshots")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> upsertSnapshot(
            @PathVariable Long orgId,
            @RequestBody CreateSnapshotRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(forecastingService.upsertSnapshot(orgId, req));
    }

    @PostMapping("/{orgId}/projections/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> generateProjections(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(forecastingService.generateProjections(orgId));
    }

    @GetMapping("/{orgId}/projections")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProjections(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(forecastingService.getProjections(orgId));
    }
}
