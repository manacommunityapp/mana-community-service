package com.manacommunity.api.communityops.asset;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class CommunityAssetController {

    private final AssetMaintenanceService service;

    @PostMapping
    public ResponseEntity<CommunityAsset> create(@RequestBody AssetDto req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(service.createAsset(principal.getCommunityId(), req));
    }

    @GetMapping
    public ResponseEntity<List<CommunityAsset>> list(
            @RequestParam(required = false) AssetCategory category,
            @RequestParam(required = false) AssetStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getAssets(principal.getCommunityId(), category, status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityAsset> update(@PathVariable Long id, @RequestBody AssetDto req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.updateAsset(id, req));
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<AssetMaintenanceLog> logMaintenance(@PathVariable Long id,
            @RequestBody MaintenanceLogDto req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(service.logMaintenance(id, req, principal.getUserId()));
    }

    @GetMapping("/{id}/maintenance")
    public ResponseEntity<List<AssetMaintenanceLog>> history(@PathVariable Long id) {
        return ResponseEntity.ok(service.getHistory(id));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<AssetMaintenanceSchedule>> overdue(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getOverdue(principal.getCommunityId()));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<AssetMaintenanceSchedule>> upcoming(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getUpcoming(principal.getCommunityId(), days));
    }
}
