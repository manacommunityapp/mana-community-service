package com.manacommunity.api.mobile.controller;

import com.manacommunity.api.mobile.entity.DeviceTelemetry;
import com.manacommunity.api.mobile.entity.ScanSession.LightingCondition;
import com.manacommunity.api.mobile.entity.WakeUpAudit.WakeStrategy;
import com.manacommunity.api.mobile.repository.DeviceTelemetryRepository;
import com.manacommunity.api.mobile.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/mobile-engineering")
@RequiredArgsConstructor
public class MobileEngineeringController {

    private final BatteryReliabilityService batteryService;
    private final CameraPipelineService cameraService;
    private final StartupOptimizationService startupService;
    private final DeviceTelemetryRepository telemetryRepo;

    // === Battery & Wake Reliability ===

    @GetMapping("/battery/report")
    public ResponseEntity<BatteryReliabilityService.WakeReliabilityReport> getWakeReport() {
        return ResponseEntity.ok(batteryService.generateReport());
    }

    @PostMapping("/battery/simulate-wake")
    public ResponseEntity<com.manacommunity.api.mobile.entity.WakeUpAudit> simulateWakeUp(
            @RequestParam String deviceId, @RequestParam WakeStrategy strategy) {
        return ResponseEntity.ok(batteryService.simulateWakeUp(deviceId, strategy));
    }

    @PostMapping("/battery/dual-channel")
    public ResponseEntity<BatteryReliabilityService.DualChannelResult> simulateDualChannel(
            @RequestParam String deviceId) {
        return ResponseEntity.ok(batteryService.simulateDualChannel(deviceId));
    }

    // === Camera Pipeline ===

    @GetMapping("/camera/config")
    public ResponseEntity<CameraPipelineService.CameraPipelineConfig> getCameraConfig(
            @RequestParam(defaultValue = "Redmi Note 12") String deviceModel,
            @RequestParam(defaultValue = "NORMAL") LightingCondition lighting) {
        return ResponseEntity.ok(cameraService.getOptimalConfig(deviceModel, lighting));
    }

    @PostMapping("/camera/simulate")
    public ResponseEntity<CameraPipelineService.ScanSimulationResult> simulateScan(
            @RequestParam(defaultValue = "Redmi Note 12") String deviceModel,
            @RequestParam(defaultValue = "NORMAL") LightingCondition lighting,
            @RequestParam(defaultValue = "10") int durationMinutes,
            @RequestParam(defaultValue = "15") int scansPerMinute) {
        return ResponseEntity.ok(cameraService.simulateScanSession(deviceModel, lighting, durationMinutes, scansPerMinute));
    }

    @GetMapping("/camera/summary")
    public ResponseEntity<CameraPipelineService.ScanPerformanceSummary> getCameraSummary() {
        return ResponseEntity.ok(cameraService.getPerformanceSummary());
    }

    // === Startup Optimization ===

    @PostMapping("/startup/analyze")
    public ResponseEntity<StartupOptimizationService.StartupAnalysis> analyzeStartup(
            @RequestBody StartupRequest req) {
        return ResponseEntity.ok(startupService.analyzeStartup(req.deviceModel(), req.ramMb(),
                req.baselineProfileEnabled(), req.lazyInjection(), req.staleWhileRevalidate()));
    }

    @GetMapping("/startup/fleet")
    public ResponseEntity<StartupOptimizationService.StartupFleetSummary> getFleetSummary() {
        return ResponseEntity.ok(startupService.getFleetSummary());
    }

    // === Demo Seed ===

    @PostMapping("/seed")
    public ResponseEntity<String> seedDemoData() {
        List<String[]> devices = List.of(
                new String[]{"xiaomi-rn12-001", "Xiaomi", "Redmi Note 12", "MIUI 14", "RESIDENT"},
                new String[]{"xiaomi-rn10-002", "Xiaomi", "Redmi Note 10", "MIUI 13", "GUARD"},
                new String[]{"vivo-y56-003", "Vivo", "Y56 5G", "FuntouchOS 13", "RESIDENT"},
                new String[]{"oppo-a78-004", "Oppo", "A78 5G", "ColorOS 13", "RESIDENT"},
                new String[]{"realme-11-005", "Realme", "Realme 11", "Realme UI 4.0", "GUARD"},
                new String[]{"samsung-a14-006", "Samsung", "Galaxy A14", "OneUI 5.1", "RESIDENT"},
                new String[]{"samsung-s23-007", "Samsung", "Galaxy S23", "OneUI 6.0", "MC_MEMBER"},
                new String[]{"oneplus-12r-008", "OnePlus", "12R", "OxygenOS 14", "RESIDENT"}
        );

        for (String[] d : devices) {
            boolean isGuard = d[4].equals("GUARD");
            int ram = switch (d[1]) {
                case "Xiaomi" -> d[2].contains("12") ? 4096 : 3072;
                case "Vivo" -> 4096;
                case "Oppo" -> 4096;
                case "Realme" -> 6144;
                case "Samsung" -> d[2].contains("S23") ? 8192 : 3072;
                default -> 8192;
            };

            telemetryRepo.save(DeviceTelemetry.builder()
                    .userId((long) (100 + devices.indexOf(new String[]{d[0]})))
                    .deviceId(d[0]).oem(d[1]).model(d[2]).osVersion(d[3]).appVersion("4.2.1")
                    .role(DeviceTelemetry.DeviceRole.valueOf(d[4]))
                    .batteryPct(20 + (int)(Math.random() * 80))
                    .batteryOptimizationExempt(Math.random() > 0.4)
                    .autoStartEnabled(d[1].equals("Samsung") || Math.random() > 0.5)
                    .backgroundRestrictionLevel(d[1].equals("Xiaomi") ? "AGGRESSIVE" : d[1].equals("Samsung") ? "ADAPTIVE" : "MODERATE")
                    .fcmTokenValid(true)
                    .fcmDeliveryChannel("FCM_HIGH_PRIORITY")
                    .lastFcmLatencyMs((long)(300 + Math.random() * 2500))
                    .coldStartMs((long)(600 + Math.random() * (ram <= 3072 ? 1800 : 800)))
                    .warmStartMs((long)(100 + Math.random() * 300))
                    .dexClassCount(ram)
                    .baselineProfileCoverage(Math.random() > 0.3 ? (long)(60 + Math.random() * 35) : 0)
                    .lazyInjectionEnabled(Math.random() > 0.4)
                    .avgFrameProcessMs(isGuard ? 25 + Math.random() * 20 : 0)
                    .qrScansPerMinute(isGuard ? 12 + (int)(Math.random() * 6) : 0)
                    .autoTorchCalibrated(isGuard)
                    .cameraInitMs(isGuard ? 180 + Math.random() * 150 : 0)
                    .thermalThrottleEvents(isGuard ? (int)(Math.random() * 5) : 0)
                    .lastHeartbeat(LocalDateTime.now().minusMinutes((long)(Math.random() * 30)))
                    .lastWakeUpSuccess(LocalDateTime.now().minusMinutes((long)(Math.random() * 60)))
                    .missedWakeUps((int)(Math.random() * (d[1].equals("Xiaomi") ? 8 : d[1].equals("Samsung") ? 1 : 5)))
                    .totalWakeAttempts(50 + (int)(Math.random() * 200))
                    .build());
        }

        return ResponseEntity.ok("Seeded 8 device telemetry records across 6 OEMs");
    }

    public record StartupRequest(String deviceModel, int ramMb, boolean baselineProfileEnabled,
                                   boolean lazyInjection, boolean staleWhileRevalidate) {}
}
