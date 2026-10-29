package com.manacommunity.api.ops.controller;

import com.manacommunity.api.ops.OpsMetricsCollector;
import com.manacommunity.api.ops.dto.OpsDashboardDtos.*;
import com.manacommunity.api.ops.service.PlatformOpsService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.manacommunity.api.constants.permissions.AdminPermissions.VIEW_ADMIN;

@RestController
@RequestMapping("/api/admin/ops")
@RequiredArgsConstructor
public class PlatformOpsController {

    private final PlatformOpsService platformOpsService;
    private final OpsMetricsCollector metricsCollector;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/overview")
    public ResponseEntity<PlatformOpsOverviewDto> getOverview(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(platformOpsService.getPlatformOverview());
    }

    @GetMapping("/services")
    public ResponseEntity<List<ServiceHealthDto>> getServices(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(platformOpsService.probeAllServices());
    }

    @GetMapping("/failures")
    public ResponseEntity<List<FailedRequestDto>> getRecentFailures(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(metricsCollector.getRecentFailures());
    }

    @GetMapping("/queues")
    public ResponseEntity<List<QueueLagDto>> getQueueLags(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(platformOpsService.collectQueueLags());
    }

    @GetMapping("/migrations")
    public ResponseEntity<List<MigrationStatusDto>> getMigrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(platformOpsService.collectMigrationStatuses());
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeIncident(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        String username = principal != null ? principal.getUsername() : "admin";
        boolean acknowledged = platformOpsService.acknowledgeIncident(id, username);
        return ResponseEntity.ok(Map.of("incidentId", id, "acknowledged", acknowledged, "by", username));
    }

    @PostMapping("/probe")
    public ResponseEntity<List<ServiceHealthDto>> triggerProbeNow(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(platformOpsService.probeAllServices());
    }
}