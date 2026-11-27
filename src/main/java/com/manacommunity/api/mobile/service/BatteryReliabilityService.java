package com.manacommunity.api.mobile.service;

import com.manacommunity.api.mobile.entity.DeviceTelemetry;
import com.manacommunity.api.mobile.entity.WakeUpAudit;
import com.manacommunity.api.mobile.entity.WakeUpAudit.WakeStrategy;
import com.manacommunity.api.mobile.repository.DeviceTelemetryRepository;
import com.manacommunity.api.mobile.repository.WakeUpAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatteryReliabilityService {

    private final DeviceTelemetryRepository telemetryRepo;
    private final WakeUpAuditRepository wakeUpRepo;

    private static final Map<String, OemProfile> OEM_PROFILES = Map.of(
            "Xiaomi", new OemProfile("Xiaomi", "MIUI", "AutoStart + Battery Saver whitelist required",
                    List.of("MIUI AutoStart kills background services within 3 min",
                            "Battery Saver aggressive mode terminates even foreground services",
                            "Recent task swipe-kill kills the app process entirely",
                            "MIUI 14+ requires explicit lock in recent apps"),
                    List.of(WakeStrategy.FCM_HIGH_PRIORITY, WakeStrategy.FOREGROUND_SERVICE, WakeStrategy.ALARM_MANAGER_EXACT),
                    82.5),
            "Vivo", new OemProfile("Vivo", "FuntouchOS", "Background power consumption management whitelist",
                    List.of("FuntouchOS restricts background CPU after 10 min",
                            "i-Manager kills apps not in whitelist overnight",
                            "High background power consumption alert dismisses app",
                            "Vivo NEX and iQOO have separate AI power manager"),
                    List.of(WakeStrategy.FCM_HIGH_PRIORITY, WakeStrategy.VOIP_PUSH_KIT, WakeStrategy.DUAL_CHANNEL),
                    78.3),
            "Oppo", new OemProfile("Oppo", "ColorOS", "App Quick Freeze exemption + Smart power saver whitelist",
                    List.of("ColorOS App Quick Freeze suspends background apps in 5 min",
                            "Super Power Saving mode kills all non-system background processes",
                            "Smart Power Saver learns usage patterns and kills infrequent apps",
                            "ColorOS 13+ deep sleep disables network for frozen apps"),
                    List.of(WakeStrategy.FCM_HIGH_PRIORITY, WakeStrategy.FOREGROUND_SERVICE, WakeStrategy.WORK_MANAGER_EXPEDITED),
                    79.1),
            "Realme", new OemProfile("Realme", "Realme UI (ColorOS fork)", "App battery optimization exemption",
                    List.of("Shares ColorOS restrictions with Oppo-specific overrides",
                            "Realme UI 4.0 aggressive doze after 2 min screen off",
                            "Smart battery optimization kills apps with <5% daily usage",
                            "Background data restriction per-app in Data Management"),
                    List.of(WakeStrategy.FCM_HIGH_PRIORITY, WakeStrategy.DUAL_CHANNEL, WakeStrategy.ALARM_MANAGER_EXACT),
                    80.7),
            "Samsung", new OemProfile("Samsung", "OneUI", "Sleeping apps + Never sleeping list",
                    List.of("OneUI moves apps to Sleeping after 3 days unused",
                            "Deep sleeping apps fully prevented from background run",
                            "Adaptive battery learns and restricts unused apps",
                            "Samsung push service (SPush) as FCM alternative"),
                    List.of(WakeStrategy.FCM_HIGH_PRIORITY, WakeStrategy.FOREGROUND_SERVICE),
                    94.2),
            "OnePlus", new OemProfile("OnePlus", "OxygenOS/ColorOS merge", "Battery optimization exemption",
                    List.of("OxygenOS 13+ merged with ColorOS restrictions",
                            "Aggressive app cleanup in memory management",
                            "Optimized charging may delay push delivery during charging"),
                    List.of(WakeStrategy.FCM_HIGH_PRIORITY, WakeStrategy.FOREGROUND_SERVICE),
                    91.8)
    );

    public WakeReliabilityReport generateReport() {
        List<DeviceTelemetry> allDevices = telemetryRepo.findAll();
        List<WakeUpAudit> allAudits = wakeUpRepo.findAll();

        // OEM-wise reliability
        Map<String, OemReliability> oemStats = new HashMap<>();
        for (var profile : OEM_PROFILES.values()) {
            List<WakeUpAudit> oemAudits = allAudits.stream()
                    .filter(a -> profile.oem().equals(a.getOem())).toList();
            long total = oemAudits.size();
            long success = oemAudits.stream().filter(WakeUpAudit::isWakeSuccessful).count();
            double reliability = total > 0 ? (double) success / total * 100 : profile.baselineReliability();

            Map<String, Double> strategyReliability = new HashMap<>();
            for (var strategy : profile.recommendedStrategies()) {
                long stratTotal = oemAudits.stream().filter(a -> a.getStrategy() == strategy).count();
                long stratSuccess = oemAudits.stream().filter(a -> a.getStrategy() == strategy && a.isWakeSuccessful()).count();
                strategyReliability.put(strategy.name(), stratTotal > 0 ? (double) stratSuccess / stratTotal * 100 : estimateStrategyReliability(profile.oem(), strategy));
            }

            double avgLatency = oemAudits.stream().filter(WakeUpAudit::isWakeSuccessful)
                    .mapToLong(WakeUpAudit::getDeliveryLatencyMs).average().orElse(estimateLatency(profile.oem()));

            oemStats.put(profile.oem(), new OemReliability(profile, reliability, total, success,
                    avgLatency, strategyReliability));
        }

        double overallReliability = allAudits.isEmpty()
                ? OEM_PROFILES.values().stream().mapToDouble(OemProfile::baselineReliability).average().orElse(85)
                : (double) allAudits.stream().filter(WakeUpAudit::isWakeSuccessful).count() / allAudits.size() * 100;

        // Strategy effectiveness ranking
        List<StrategyRanking> rankings = Arrays.stream(WakeStrategy.values()).map(s -> {
            List<WakeUpAudit> stratAudits = allAudits.stream().filter(a -> a.getStrategy() == s).toList();
            long t = stratAudits.size();
            long succ = stratAudits.stream().filter(WakeUpAudit::isWakeSuccessful).count();
            double rel = t > 0 ? (double) succ / t * 100 : estimateGlobalStrategyReliability(s);
            double lat = stratAudits.stream().filter(WakeUpAudit::isWakeSuccessful)
                    .mapToLong(WakeUpAudit::getDeliveryLatencyMs).average().orElse(estimateGlobalLatency(s));
            return new StrategyRanking(s.name(), rel, lat, t, getStrategyDescription(s));
        }).sorted(Comparator.comparingDouble(StrategyRanking::reliability).reversed()).toList();

        // Devices with missed wake-ups
        List<DeviceTelemetry> problematic = telemetryRepo.findDevicesWithMissedWakeUps();

        return new WakeReliabilityReport(overallReliability, oemStats, rankings,
                allDevices.size(), allAudits.size(), problematic.size());
    }

    public WakeUpAudit simulateWakeUp(String deviceId, WakeStrategy strategy) {
        DeviceTelemetry device = telemetryRepo.findByDeviceId(deviceId).orElse(null);
        String oem = device != null ? device.getOem() : "Samsung";
        String model = device != null ? device.getModel() : "Galaxy S23";

        OemProfile profile = OEM_PROFILES.getOrDefault(oem, OEM_PROFILES.get("Samsung"));
        double baseReliability = estimateStrategyReliability(oem, strategy) / 100.0;
        boolean success = Math.random() < baseReliability;
        long latencyMs = success ? (long) (200 + Math.random() * 2800) : 0;

        WakeUpAudit audit = WakeUpAudit.builder()
                .deviceId(deviceId)
                .oem(oem)
                .model(model)
                .strategy(strategy)
                .pushPriority(WakeUpAudit.PushPriority.HIGH)
                .deliveryLatencyMs(latencyMs)
                .wakeSuccessful(success)
                .failureReason(success ? null : getFailureReason(oem, strategy))
                .foregroundServiceActive(strategy == WakeStrategy.FOREGROUND_SERVICE)
                .voipPushUsed(strategy == WakeStrategy.VOIP_PUSH_KIT)
                .retryAttempts(success ? 0 : 1 + new Random().nextInt(3))
                .sentAt(LocalDateTime.now())
                .deliveredAt(success ? LocalDateTime.now().plusNanos(latencyMs * 1_000_000) : null)
                .build();

        if (device != null) {
            device.setTotalWakeAttempts(device.getTotalWakeAttempts() + 1);
            if (!success) device.setMissedWakeUps(device.getMissedWakeUps() + 1);
            else device.setLastWakeUpSuccess(LocalDateTime.now());
            device.setLastHeartbeat(LocalDateTime.now());
            telemetryRepo.save(device);
        }

        return wakeUpRepo.save(audit);
    }

    public DualChannelResult simulateDualChannel(String deviceId) {
        WakeUpAudit fcm = simulateWakeUp(deviceId, WakeStrategy.FCM_HIGH_PRIORITY);
        WakeUpAudit backup;
        DeviceTelemetry device = telemetryRepo.findByDeviceId(deviceId).orElse(null);
        String oem = device != null ? device.getOem() : "Samsung";

        if (List.of("Vivo", "Oppo").contains(oem)) {
            backup = simulateWakeUp(deviceId, WakeStrategy.VOIP_PUSH_KIT);
        } else {
            backup = simulateWakeUp(deviceId, WakeStrategy.FOREGROUND_SERVICE);
        }

        boolean combinedSuccess = fcm.isWakeSuccessful() || backup.isWakeSuccessful();
        long effectiveLatency = fcm.isWakeSuccessful() && backup.isWakeSuccessful()
                ? Math.min(fcm.getDeliveryLatencyMs(), backup.getDeliveryLatencyMs())
                : fcm.isWakeSuccessful() ? fcm.getDeliveryLatencyMs() : backup.getDeliveryLatencyMs();

        return new DualChannelResult(fcm, backup, combinedSuccess, effectiveLatency,
                "FCM " + (fcm.isWakeSuccessful() ? "delivered" : "blocked") + " + " +
                        backup.getStrategy().name() + " " + (backup.isWakeSuccessful() ? "delivered" : "blocked"));
    }

    private double estimateStrategyReliability(String oem, WakeStrategy strategy) {
        return switch (oem) {
            case "Xiaomi" -> switch (strategy) {
                case FCM_HIGH_PRIORITY -> 72; case FOREGROUND_SERVICE -> 91;
                case ALARM_MANAGER_EXACT -> 85; case DUAL_CHANNEL -> 97; default -> 60;
            };
            case "Vivo" -> switch (strategy) {
                case FCM_HIGH_PRIORITY -> 68; case VOIP_PUSH_KIT -> 94;
                case DUAL_CHANNEL -> 98; default -> 55;
            };
            case "Oppo" -> switch (strategy) {
                case FCM_HIGH_PRIORITY -> 70; case FOREGROUND_SERVICE -> 89;
                case WORK_MANAGER_EXPEDITED -> 82; case DUAL_CHANNEL -> 96; default -> 58;
            };
            case "Realme" -> switch (strategy) {
                case FCM_HIGH_PRIORITY -> 71; case DUAL_CHANNEL -> 97;
                case ALARM_MANAGER_EXACT -> 84; default -> 62;
            };
            default -> switch (strategy) {
                case FCM_HIGH_PRIORITY -> 95; case FOREGROUND_SERVICE -> 97;
                case DUAL_CHANNEL -> 99.5; default -> 90;
            };
        };
    }

    private double estimateLatency(String oem) {
        return switch (oem) {
            case "Xiaomi" -> 1800; case "Vivo" -> 2100; case "Oppo" -> 1950;
            case "Realme" -> 1900; default -> 800;
        };
    }

    private double estimateGlobalStrategyReliability(WakeStrategy s) {
        return switch (s) {
            case FCM_HIGH_PRIORITY -> 82; case FCM_DATA_MESSAGE -> 65;
            case FOREGROUND_SERVICE -> 93; case VOIP_PUSH_KIT -> 91;
            case WEBSOCKET_KEEPALIVE -> 75; case ALARM_MANAGER_EXACT -> 85;
            case WORK_MANAGER_EXPEDITED -> 80; case DUAL_CHANNEL -> 98;
        };
    }

    private double estimateGlobalLatency(WakeStrategy s) {
        return switch (s) {
            case FCM_HIGH_PRIORITY -> 1200; case FCM_DATA_MESSAGE -> 2500;
            case FOREGROUND_SERVICE -> 300; case VOIP_PUSH_KIT -> 400;
            case WEBSOCKET_KEEPALIVE -> 150; case ALARM_MANAGER_EXACT -> 500;
            case WORK_MANAGER_EXPEDITED -> 800; case DUAL_CHANNEL -> 350;
        };
    }

    private String getFailureReason(String oem, WakeStrategy strategy) {
        return switch (oem) {
            case "Xiaomi" -> "MIUI AutoStart killed background process";
            case "Vivo" -> "FuntouchOS i-Manager terminated app";
            case "Oppo" -> "ColorOS App Quick Freeze suspended process";
            case "Realme" -> "Realme UI aggressive doze blocked delivery";
            default -> "Process not running, FCM delivery failed";
        };
    }

    private String getStrategyDescription(WakeStrategy s) {
        return switch (s) {
            case FCM_HIGH_PRIORITY -> "Firebase Cloud Messaging with high priority flag — bypasses doze on stock Android";
            case FCM_DATA_MESSAGE -> "FCM data-only message — no notification tray, app handles in onMessageReceived";
            case FOREGROUND_SERVICE -> "Persistent foreground notification with ongoing service — survives most OEM kills";
            case VOIP_PUSH_KIT -> "VoIP push (iOS CallKit / Huawei PushKit) — highest OS priority, wakes from deep sleep";
            case WEBSOCKET_KEEPALIVE -> "Persistent WebSocket with TCP keepalive — lowest latency but highest battery cost";
            case ALARM_MANAGER_EXACT -> "AlarmManager.setExactAndAllowWhileIdle — scheduled exact wake, 9-min min interval in doze";
            case WORK_MANAGER_EXPEDITED -> "WorkManager with expedited flag — system-scheduled but respects battery constraints";
            case DUAL_CHANNEL -> "FCM + backup channel (VoIP/foreground) simultaneously — >99.5% combined reliability";
        };
    }

    public record OemProfile(String oem, String skin, String requiredAction, List<String> killBehaviors,
                              List<WakeStrategy> recommendedStrategies, double baselineReliability) {}
    public record OemReliability(OemProfile profile, double reliability, long totalAttempts, long successCount,
                                  double avgLatencyMs, Map<String, Double> strategyReliability) {}
    public record StrategyRanking(String strategy, double reliability, double avgLatencyMs,
                                   long totalAttempts, String description) {}
    public record WakeReliabilityReport(double overallReliability, Map<String, OemReliability> oemStats,
                                         List<StrategyRanking> strategyRankings, int totalDevices,
                                         long totalAudits, int problematicDevices) {}
    public record DualChannelResult(WakeUpAudit primary, WakeUpAudit backup, boolean combinedSuccess,
                                     long effectiveLatencyMs, String summary) {}
}
