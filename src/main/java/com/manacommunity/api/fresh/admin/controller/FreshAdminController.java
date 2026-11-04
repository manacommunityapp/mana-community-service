package com.manacommunity.api.fresh.admin.controller;

import com.manacommunity.api.fresh.vendor.dto.ApproveCapabilityRequest;
import com.manacommunity.api.fresh.vendor.dto.VendorCapabilityDto;
import com.manacommunity.api.fresh.vendor.service.FreshVendorCapabilityService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fresh/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class FreshAdminController {

    private final FreshVendorCapabilityService capabilityService;
    private final com.manacommunity.api.fresh.pilot.service.FreshPilotDataSeeder pilotDataSeeder;

    @GetMapping("/capabilities/pending")
    public ResponseEntity<List<VendorCapabilityDto>> getPendingCapabilities() {
        return ResponseEntity.ok(capabilityService.getPendingCapabilities());
    }

    @GetMapping("/vendors/{vendorId}/capabilities")
    public ResponseEntity<List<VendorCapabilityDto>> getVendorCapabilities(
            @PathVariable Long vendorId) {
        return ResponseEntity.ok(capabilityService.getCapabilitiesByVendor(vendorId));
    }

    @PostMapping("/capabilities/{capabilityId}/review")
    public ResponseEntity<VendorCapabilityDto> reviewCapability(
            @PathVariable Long capabilityId,
            @Valid @RequestBody ApproveCapabilityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long adminUserId = principal != null ? principal.getId() : 1L;
        return ResponseEntity.ok(capabilityService.reviewCapability(capabilityId, request, adminUserId));
    }

    @PostMapping("/capabilities/{capabilityId}/suspend")
    public ResponseEntity<VendorCapabilityDto> suspendCapability(
            @PathVariable Long capabilityId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long adminUserId = principal != null ? principal.getId() : 1L;
        String reason = body != null ? body.getOrDefault("reason", "Administrative suspension") : "Administrative suspension";
        return ResponseEntity.ok(capabilityService.suspendCapability(capabilityId, reason, adminUserId));
    }

    @PostMapping("/pilot/seed")
    public ResponseEntity<Map<String, Object>> seedPilotEnvironment() {
        return ResponseEntity.ok(pilotDataSeeder.seedStage12PilotData());
    }

    @GetMapping("/pilot/metrics")
    public ResponseEntity<com.manacommunity.api.fresh.pilot.dto.FreshPilotMetricsDto> getPilotMetrics() {
        return ResponseEntity.ok(pilotDataSeeder.getPilotMetrics());
    }
}

