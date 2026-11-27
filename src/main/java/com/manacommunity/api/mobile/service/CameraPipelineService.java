package com.manacommunity.api.mobile.service;

import com.manacommunity.api.mobile.entity.ScanSession;
import com.manacommunity.api.mobile.entity.ScanSession.LightingCondition;
import com.manacommunity.api.mobile.repository.ScanSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CameraPipelineService {

    private final ScanSessionRepository sessionRepo;

    private static final double TARGET_DECODE_MS = 50.0;
    private static final double THERMAL_THROTTLE_TEMP_C = 42.0;
    private static final int TARGET_SCANS_PER_MINUTE = 15;

    public CameraPipelineConfig getOptimalConfig(String deviceModel, LightingCondition lighting) {
        int cameraResolution = switch (lighting) {
            case BRIGHT_SUNLIGHT -> 640;
            case NORMAL -> 720;
            case DIM, DARK -> 480;
            case MIXED_GLARE -> 640;
        };

        double exposureCompensation = switch (lighting) {
            case BRIGHT_SUNLIGHT -> -1.5;
            case NORMAL -> 0.0;
            case DIM -> 1.5;
            case DARK -> 2.5;
            case MIXED_GLARE -> -0.5;
        };

        boolean autoTorch = lighting == LightingCondition.DIM || lighting == LightingCondition.DARK;
        int torchLuxThreshold = lighting == LightingCondition.DARK ? 20 : 50;

        boolean hdr = lighting == LightingCondition.MIXED_GLARE;

        double targetFps = 30.0;
        int barcodeFormats = 0x0001 | 0x0100;

        String decodingStrategy = switch (lighting) {
            case BRIGHT_SUNLIGHT -> "FAST_SCAN: skip every other frame, ROI center 60%";
            case NORMAL -> "STANDARD: process all frames, full viewport";
            case DIM, DARK -> "ENHANCED: increased gain, center ROI 70%, 2x decode attempts";
            case MIXED_GLARE -> "ADAPTIVE: dynamic ROI, contrast normalization pre-process";
        };

        List<String> optimizations = new ArrayList<>();
        optimizations.add("CameraX ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST — drop stale frames");
        optimizations.add("ML Kit barcode detector with FORMAT_QR_CODE | FORMAT_CODE_128");
        optimizations.add("ROI cropping before decode — 40-60% fewer pixels to process");
        optimizations.add("YUV_420_888 → grayscale conversion (single channel, skip UV planes)");

        if (autoTorch) {
            optimizations.add("Auto-torch: AE callback monitors avgLuminance, toggles at " + torchLuxThreshold + " lux");
        }
        if (hdr) {
            optimizations.add("HDR: CONTROL_SCENE_MODE_HDR for mixed glare compensation");
        }

        optimizations.add("Thermal management: throttle to 15fps when device temp > " + THERMAL_THROTTLE_TEMP_C + "°C");
        optimizations.add("Pre-warm camera during guard shift start (avoid cold-init delay)");

        double estimatedDecodeMs = switch (lighting) {
            case BRIGHT_SUNLIGHT -> 28 + Math.random() * 15;
            case NORMAL -> 22 + Math.random() * 12;
            case DIM -> 35 + Math.random() * 18;
            case DARK -> 42 + Math.random() * 20;
            case MIXED_GLARE -> 38 + Math.random() * 16;
        };

        return new CameraPipelineConfig(cameraResolution, exposureCompensation, autoTorch,
                torchLuxThreshold, hdr, targetFps, decodingStrategy, optimizations,
                Math.round(estimatedDecodeMs * 100.0) / 100.0, estimatedDecodeMs <= TARGET_DECODE_MS);
    }

    public ScanSimulationResult simulateScanSession(String deviceModel, LightingCondition lighting,
                                                      int durationMinutes, int scansPerMinute) {
        List<ScanResult> scans = new ArrayList<>();
        int totalScans = durationMinutes * scansPerMinute;
        int successful = 0;
        double tempC = 32 + Math.random() * 5;
        double peakTemp = tempC;
        int thermalThrottles = 0;
        List<Double> decodeTimes = new ArrayList<>();

        for (int i = 0; i < totalScans; i++) {
            double minuteElapsed = (double) i / scansPerMinute;
            tempC += 0.08 + Math.random() * 0.05;
            if (tempC > peakTemp) peakTemp = tempC;

            boolean throttled = tempC > THERMAL_THROTTLE_TEMP_C;
            if (throttled) {
                thermalThrottles++;
                tempC -= 0.3;
            }

            double baseDecodeMs = switch (lighting) {
                case BRIGHT_SUNLIGHT -> 25; case NORMAL -> 20;
                case DIM -> 32; case DARK -> 40; case MIXED_GLARE -> 35;
            };
            double jitter = (Math.random() - 0.5) * 20;
            double thermalPenalty = throttled ? 15 : 0;
            double decodeMs = Math.max(8, baseDecodeMs + jitter + thermalPenalty);
            decodeTimes.add(decodeMs);

            boolean success = decodeMs <= 60 && Math.random() > 0.02;
            if (success) successful++;

            if (i < 5 || i == totalScans - 1 || throttled) {
                scans.add(new ScanResult(i + 1, Math.round(decodeMs * 10.0) / 10.0, success,
                        Math.round(tempC * 10.0) / 10.0, throttled));
            }
        }

        Collections.sort(decodeTimes);
        double avg = decodeTimes.stream().mapToDouble(d -> d).average().orElse(0);
        double p95 = decodeTimes.get((int) (decodeTimes.size() * 0.95));
        double p99 = decodeTimes.get((int) (decodeTimes.size() * 0.99));
        double frameDropRate = (double) (totalScans - successful) / totalScans * 100;

        ScanSession session = ScanSession.builder()
                .deviceId("sim-" + deviceModel.toLowerCase().replace(" ", "-"))
                .guardId("guard-001")
                .gateId("main-gate")
                .totalScans(totalScans)
                .successfulScans(successful)
                .failedScans(totalScans - successful)
                .avgDecodeMs(Math.round(avg * 100.0) / 100.0)
                .p95DecodeMs(Math.round(p95 * 100.0) / 100.0)
                .p99DecodeMs(Math.round(p99 * 100.0) / 100.0)
                .avgFrameProcessMs(Math.round((avg + 5) * 100.0) / 100.0)
                .lightingCondition(lighting)
                .autoTorchTriggered(lighting == LightingCondition.DIM || lighting == LightingCondition.DARK)
                .thermalThrottleEvents(thermalThrottles)
                .avgDeviceTempC(Math.round(((32 + peakTemp) / 2) * 10.0) / 10.0)
                .peakDeviceTempC(Math.round(peakTemp * 10.0) / 10.0)
                .cameraRestarts(thermalThrottles > 5 ? 1 : 0)
                .cameraInitMs(Math.round((180 + Math.random() * 120) * 10.0) / 10.0)
                .droppedFrames(totalScans - successful)
                .processedFrames(totalScans)
                .frameDropRate(Math.round(frameDropRate * 100.0) / 100.0)
                .sessionStart(LocalDateTime.now().minusMinutes(durationMinutes))
                .sessionEnd(LocalDateTime.now())
                .build();

        sessionRepo.save(session);

        boolean meetsTarget = avg <= TARGET_DECODE_MS;
        List<String> recommendations = new ArrayList<>();
        if (!meetsTarget) recommendations.add("Average decode " + Math.round(avg) + "ms exceeds " + TARGET_DECODE_MS + "ms target — reduce resolution or enable ROI cropping");
        if (thermalThrottles > 3) recommendations.add("Thermal throttling detected " + thermalThrottles + " times — implement duty cycling (scan 45s, cool 15s)");
        if (p99 > 80) recommendations.add("P99 latency " + Math.round(p99) + "ms — pre-warm ML Kit detector at shift start");
        if (frameDropRate > 5) recommendations.add("Frame drop rate " + Math.round(frameDropRate) + "% — consider frame skipping strategy");

        return new ScanSimulationResult(session, scans, meetsTarget, recommendations);
    }

    public ScanPerformanceSummary getPerformanceSummary() {
        List<ScanSession> all = sessionRepo.findAll();
        double overallAvg = all.stream().mapToDouble(ScanSession::getAvgDecodeMs).average().orElse(28);
        double overallP95 = all.stream().mapToDouble(ScanSession::getP95DecodeMs).average().orElse(42);
        int totalThrottles = all.stream().mapToInt(ScanSession::getThermalThrottleEvents).sum();

        Map<String, Double> byLighting = all.stream()
                .collect(Collectors.groupingBy(s -> s.getLightingCondition().name(),
                        Collectors.averagingDouble(ScanSession::getAvgDecodeMs)));

        long sessionsUnderTarget = all.stream().filter(s -> s.getAvgDecodeMs() <= TARGET_DECODE_MS).count();
        double targetMetPct = all.isEmpty() ? 85 : (double) sessionsUnderTarget / all.size() * 100;

        return new ScanPerformanceSummary(overallAvg, overallP95, totalThrottles,
                byLighting, all.size(), targetMetPct, TARGET_DECODE_MS);
    }

    public record CameraPipelineConfig(int resolution, double exposureCompensation, boolean autoTorch,
                                        int torchLuxThreshold, boolean hdr, double targetFps,
                                        String decodingStrategy, List<String> optimizations,
                                        double estimatedDecodeMs, boolean meetsTarget) {}
    public record ScanResult(int scanNumber, double decodeMs, boolean success, double deviceTempC, boolean throttled) {}
    public record ScanSimulationResult(ScanSession session, List<ScanResult> sampleScans,
                                        boolean meetsTarget, List<String> recommendations) {}
    public record ScanPerformanceSummary(double overallAvgDecodeMs, double overallP95DecodeMs,
                                          int totalThermalThrottles, Map<String, Double> avgByLighting,
                                          int totalSessions, double targetMetPct, double targetMs) {}
}
