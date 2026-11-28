package com.manacommunity.api.iot.edge.controller;

import com.manacommunity.api.iot.edge.entity.EdgeGatewayDevice;
import com.manacommunity.api.iot.edge.entity.PrepaidMeterAccount;
import com.manacommunity.api.iot.edge.repository.EdgeGatewayDeviceRepository;
import com.manacommunity.api.iot.edge.repository.PrepaidMeterAccountRepository;
import com.manacommunity.api.iot.edge.service.BarrierAccessService;
import com.manacommunity.api.iot.edge.service.DeviceHeartbeatService;
import com.manacommunity.api.iot.edge.service.PrepaidMeterDeductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iot-edge")
@RequiredArgsConstructor
public class IoTEdgeController {

    private final DeviceHeartbeatService heartbeatService;
    private final PrepaidMeterDeductionService meterService;
    private final BarrierAccessService barrierService;
    private final EdgeGatewayDeviceRepository deviceRepository;
    private final PrepaidMeterAccountRepository meterAccountRepository;

    // ── Device Heartbeat & Monitoring ──

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, String>> receiveHeartbeat(@RequestBody HeartbeatRequest request) {
        heartbeatService.processHeartbeat(request.serialNumber(), request.latencyMs(),
                request.cpuPct(), request.memoryPct(), request.uptimeSeconds(),
                request.firmwareVersion(), request.signalStrength());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/heartbeat/sweep")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<DeviceHeartbeatService.SweepResult> runOfflineSweep() {
        return ResponseEntity.ok(heartbeatService.runOfflineDetectionSweep());
    }

    @GetMapping("/devices/{communityId}/health")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<DeviceHeartbeatService.DeviceHealthSummary> getDeviceHealth(
            @PathVariable Long communityId) {
        return ResponseEntity.ok(heartbeatService.getHealthSummary(communityId));
    }

    @GetMapping("/devices/{communityId}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<EdgeGatewayDevice>> getDevices(@PathVariable Long communityId) {
        return ResponseEntity.ok(deviceRepository.findByCommunityId(communityId));
    }

    @PostMapping("/devices")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<EdgeGatewayDevice> registerDevice(@RequestBody EdgeGatewayDevice device) {
        device.setCreatedAt(Instant.now());
        device.setIsOnline(true);
        device.setConsecutiveMisses(0);
        return ResponseEntity.ok(deviceRepository.save(device));
    }

    // ── Prepaid Meter Deductions ──

    @PostMapping("/meter/telemetry")
    public ResponseEntity<PrepaidMeterDeductionService.DeductionResult> processTelemetry(
            @RequestBody TelemetryRequest request) {
        PrepaidMeterDeductionService.DeductionResult result = meterService.processTelemetryReading(
                request.meterId(), request.sequenceNumber(), request.deltaConsumption(), request.readingTimestamp());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/meter/{meterId}/recharge")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PrepaidMeterAccount> rechargeMeter(
            @PathVariable Long meterId, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(meterService.rechargeAccount(meterId, amount));
    }

    @GetMapping("/meter/{communityId}/accounts")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<PrepaidMeterAccount>> getMeterAccounts(@PathVariable Long communityId) {
        return ResponseEntity.ok(meterService.getAccountsByCommunity(communityId));
    }

    @GetMapping("/meter/low-balance-alerts")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<PrepaidMeterDeductionService.LowBalanceAlert>> getLowBalanceAlerts() {
        return ResponseEntity.ok(meterService.checkLowBalanceAlerts());
    }

    @PostMapping("/meter/accounts")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PrepaidMeterAccount> createMeterAccount(@RequestBody PrepaidMeterAccount account) {
        account.setCreatedAt(Instant.now());
        return ResponseEntity.ok(meterAccountRepository.save(account));
    }

    // ── Barrier Access Control ──

    @GetMapping("/barrier/validate")
    public ResponseEntity<BarrierAccessService.BarrierDecision> validateBarrierAccess(
            @RequestParam Long communityId,
            @RequestParam String credentialType,
            @RequestParam String credentialValue) {
        return ResponseEntity.ok(barrierService.validateAccess(communityId, credentialType, credentialValue));
    }

    @PostMapping("/barrier/{communityId}/sync-cache")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, String>> syncBarrierCache(
            @PathVariable Long communityId,
            @RequestBody List<BarrierAccessService.CacheEntry> entries) {
        barrierService.syncCacheForCommunity(communityId, entries);
        return ResponseEntity.ok(Map.of("status", "synced", "count", String.valueOf(entries.size())));
    }

    @GetMapping("/barrier/{communityId}/cache-stats")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BarrierAccessService.CacheStats> getBarrierCacheStats(@PathVariable Long communityId) {
        return ResponseEntity.ok(barrierService.getCacheStats(communityId));
    }

    // ── Request DTOs ──

    public record HeartbeatRequest(String serialNumber, Integer latencyMs, Integer cpuPct,
                                    Integer memoryPct, Long uptimeSeconds, String firmwareVersion,
                                    Integer signalStrength) {}

    public record TelemetryRequest(Long meterId, long sequenceNumber, BigDecimal deltaConsumption,
                                    Instant readingTimestamp) {}
}
