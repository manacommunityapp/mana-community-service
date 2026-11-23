package com.manacommunity.platform.ops;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/ops")
@PreAuthorize("hasAnyRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class PlatformOpsController {

    private final PlatformOpsService service;

    @GetMapping("/snapshot")
    public ResponseEntity<PlatformOpsSnapshot> snapshot() {
        return ResponseEntity.ok(service.getSnapshot());
    }

    @GetMapping("/api-health")
    public ResponseEntity<PlatformOpsSnapshot.ApiHealthMetrics> apiHealth() {
        return ResponseEntity.ok(service.getApiHealth());
    }

    @GetMapping("/jobs")
    public ResponseEntity<PlatformOpsSnapshot.BackgroundJobStats> jobs() {
        return ResponseEntity.ok(service.getJobStats());
    }

    @GetMapping("/db-pool")
    public ResponseEntity<PlatformOpsSnapshot.DbPoolMetrics> dbPool() {
        return ResponseEntity.ok(service.getDbPool());
    }

    @GetMapping("/notifications")
    public ResponseEntity<PlatformOpsSnapshot.NotificationStats> notifications() {
        return ResponseEntity.ok(service.getNotificationStats());
    }

    @GetMapping("/payments")
    public ResponseEntity<PlatformOpsSnapshot.PaymentHealthStats> payments() {
        return ResponseEntity.ok(service.getPaymentHealth());
    }

    @GetMapping("/storage")
    public ResponseEntity<PlatformOpsSnapshot.StorageStats> storage() {
        return ResponseEntity.ok(service.getStorageStats());
    }

    @GetMapping("/security")
    public ResponseEntity<List<PlatformOpsSnapshot.SecurityIncidentSummary>> security(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(service.getSecurityIncidents(hours));
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<PlatformOpsSnapshot.TenantHealthSummary>> tenants() {
        return ResponseEntity.ok(service.getTenantHealth());
    }

    // ─── Alerts ─────────────────────────────────────────────────────────────

    @GetMapping("/alerts")
    public ResponseEntity<List<PlatformOpsSnapshot.AlertDto>> alerts() {
        return ResponseEntity.ok(service.getOpenAlerts());
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long alertId,
            @RequestParam Long resolvedBy) {
        service.resolveAlert(alertId, resolvedBy);
        return ResponseEntity.ok().build();
    }

    // ─── Deploys ─────────────────────────────────────────────────────────────

    @GetMapping("/deploys")
    public ResponseEntity<List<PlatformOpsSnapshot.DeployEventDto>> deploys(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(service.getRecentDeploys(limit));
    }

    @PostMapping("/deploys")
    public ResponseEntity<PlatformDeployEvent> recordDeploy(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.recordDeploy(
            body.get("service"), body.get("version"), body.getOrDefault("env", "PRODUCTION"),
            body.getOrDefault("status", "STARTED"), body.get("commitSha")));
    }

    // ─── Metrics History ────────────────────────────────────────────────────

    @GetMapping("/metrics/{type}/history")
    public ResponseEntity<List<PlatformMetricsHistory>> metricHistory(
            @PathVariable String type,
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(service.getMetricHistory(type, hours));
    }

    @PostMapping("/metrics")
    public ResponseEntity<Void> recordMetric(@RequestBody Map<String, Object> body) {
        service.recordMetric(
            (String) body.get("type"),
            new BigDecimal(body.get("value").toString()),
            (String) body.getOrDefault("unit", ""));
        return ResponseEntity.ok().build();
    }
}
