package com.manacommunity.api.mobile.service;

import com.manacommunity.api.mobile.entity.DeviceTelemetry;
import com.manacommunity.api.mobile.repository.DeviceTelemetryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StartupOptimizationService {

    private final DeviceTelemetryRepository telemetryRepo;

    private static final long COLD_START_TARGET_MS = 800;

    public StartupAnalysis analyzeStartup(String deviceModel, int ramMb, boolean baselineProfileEnabled,
                                            boolean lazyInjection, boolean staleWhileRevalidate) {
        // Baseline cold start estimation
        double baseColdStart;
        if (ramMb <= 2048) {
            baseColdStart = 2200;
        } else if (ramMb <= 3072) {
            baseColdStart = 1600;
        } else if (ramMb <= 4096) {
            baseColdStart = 1200;
        } else if (ramMb <= 6144) {
            baseColdStart = 900;
        } else {
            baseColdStart = 700;
        }

        List<OptimizationStep> steps = new ArrayList<>();
        double currentMs = baseColdStart;

        // Step 1: Baseline Profiles
        double bpSaving = baseColdStart * 0.30;
        if (baselineProfileEnabled) {
            currentMs -= bpSaving;
            steps.add(new OptimizationStep("Baseline Profile (AOT Compilation)", bpSaving,
                    currentMs, true,
                    "Pre-compiles hot methods during install via ProfileInstaller. " +
                    "Collects traces from Macrobenchmark → baseline-prof.txt → bundled in APK. " +
                    "ART compiles listed methods on install instead of JIT at runtime.",
                    List.of("androidx.profileinstaller:profileinstaller for auto-install",
                            "Macrobenchmark module to collect startup traces",
                            "BaselineProfileRule generates baseline-prof.txt",
                            "R8 full mode compatible — methods stay after shrinking")));
        } else {
            steps.add(new OptimizationStep("Baseline Profile (NOT enabled)", 0,
                    currentMs, false,
                    "30% cold start reduction available. All methods JIT-compiled on first use.",
                    List.of("Add profileinstaller dependency", "Create Macrobenchmark startup test",
                            "Generate baseline-prof.txt", "Include in app build.gradle")));
        }

        // Step 2: Lazy Dependency Injection
        double diSaving = currentMs * 0.20;
        if (lazyInjection) {
            currentMs -= diSaving;
            steps.add(new OptimizationStep("Lazy Dependency Injection", diSaving,
                    currentMs, true,
                    "Dagger/Hilt Lazy<T> and Provider<T> for non-critical dependencies. " +
                    "Only gate-approval and home-screen dependencies initialize at startup.",
                    List.of("@Inject Lazy<AnalyticsTracker> — deferred until first use",
                            "Split AppComponent into CriticalComponent + DeferredComponent",
                            "IdlingResource for test synchronization with lazy singletons",
                            "Startup tracing via app.startupTrace() to verify deferral")));
        } else {
            steps.add(new OptimizationStep("Lazy DI (NOT enabled)", 0,
                    currentMs, false,
                    "20% reduction available. All Dagger components initialized at Application.onCreate.",
                    List.of("Convert @Inject T to @Inject Lazy<T> for non-critical deps",
                            "Move analytics, crash reporting, feature flags to lazy init")));
        }

        // Step 3: Stale-While-Revalidate Cache
        double cacheSaving = currentMs * 0.15;
        if (staleWhileRevalidate) {
            currentMs -= cacheSaving;
            steps.add(new OptimizationStep("Stale-While-Revalidate Cache", cacheSaving,
                    currentMs, true,
                    "Show cached home screen immediately while fetching fresh data in background. " +
                    "Room DB holds last-known state; OkHttp CacheControl.FORCE_CACHE for instant load.",
                    List.of("Room entity with @ColumnInfo(name = \"cached_at\") timestamp",
                            "Repository pattern: emit cached → fetch network → emit fresh",
                            "OkHttp interceptor: stale data served for 5 min, background refresh",
                            "Skeleton UI during revalidation phase")));
        } else {
            steps.add(new OptimizationStep("SWR Cache (NOT enabled)", 0,
                    currentMs, false,
                    "15% reduction available. App blocks on network request at startup.",
                    List.of("Add Room cache layer for home screen data",
                            "Implement SWR pattern in repository layer")));
        }

        // Step 4: Additional optimizations always available
        double additionalSaving = currentMs * 0.10;
        currentMs -= additionalSaving;
        steps.add(new OptimizationStep("Additional Optimizations", additionalSaving,
                currentMs, true,
                "Combined effect of class preloading, ContentProvider removal, and view inflation optimization.",
                List.of("Remove unnecessary ContentProviders (WorkManager, Firebase) from manifest merge",
                        "AsyncLayoutInflater for below-fold views",
                        "Compose: skip composition of off-screen tabs",
                        "SplashScreen API with animated vector drawable (perceived speed)")));

        boolean meetsTarget = currentMs <= COLD_START_TARGET_MS;
        double totalReduction = baseColdStart - currentMs;
        double reductionPct = (totalReduction / baseColdStart) * 100;

        // Device tier analysis
        String deviceTier = ramMb <= 2048 ? "LOW_END" : ramMb <= 4096 ? "MID_RANGE" : "HIGH_END";
        List<String> tierRecommendations = switch (deviceTier) {
            case "LOW_END" -> List.of(
                    "Enable R8 full mode for aggressive shrinking (reduce DEX count)",
                    "Disable animations on low-RAM devices (Developer Options proxy)",
                    "Use WindowBackground with theme instead of splash layout",
                    "Limit home screen to 1 API call with composite DTO",
                    "Consider App Bundle with on-demand feature delivery");
            case "MID_RANGE" -> List.of(
                    "Standard baseline profile sufficient",
                    "Lazy load analytics and feature flag SDKs",
                    "Pre-fetch gate approval data in WorkManager before app open");
            default -> List.of(
                    "Full animation suite supported",
                    "Proactive pre-loading of secondary screens",
                    "WebSocket connection can initialize at startup");
        };

        return new StartupAnalysis(deviceModel, ramMb, deviceTier, baseColdStart, currentMs,
                totalReduction, reductionPct, meetsTarget, COLD_START_TARGET_MS, steps, tierRecommendations);
    }

    public StartupFleetSummary getFleetSummary() {
        List<DeviceTelemetry> all = telemetryRepo.findAll();

        double avgColdStart = all.stream().mapToLong(DeviceTelemetry::getColdStartMs).average().orElse(1100);
        long devicesUnderTarget = all.stream().filter(d -> d.getColdStartMs() <= COLD_START_TARGET_MS).count();
        double targetMetPct = all.isEmpty() ? 65 : (double) devicesUnderTarget / all.size() * 100;

        Map<String, Double> byOem = all.stream()
                .collect(Collectors.groupingBy(DeviceTelemetry::getOem,
                        Collectors.averagingLong(DeviceTelemetry::getColdStartMs)));

        Map<String, Long> byTier = all.stream()
                .collect(Collectors.groupingBy(d -> {
                    long ram = d.getDexClassCount();
                    return ram <= 2048 ? "LOW_END" : ram <= 4096 ? "MID_RANGE" : "HIGH_END";
                }, Collectors.counting()));

        long baselineEnabled = all.stream().filter(d -> d.getBaselineProfileCoverage() > 0).count();
        long lazyEnabled = all.stream().filter(DeviceTelemetry::isLazyInjectionEnabled).count();

        List<DeviceTelemetry> slowest = telemetryRepo.findSlowStartDevices(COLD_START_TARGET_MS);

        return new StartupFleetSummary(avgColdStart, targetMetPct, COLD_START_TARGET_MS,
                byOem, byTier, all.size(), baselineEnabled, lazyEnabled, slowest.size());
    }

    public record OptimizationStep(String name, double savingMs, double resultMs, boolean enabled,
                                     String description, List<String> implementation) {}
    public record StartupAnalysis(String deviceModel, int ramMb, String deviceTier,
                                    double baselineColdStartMs, double optimizedColdStartMs,
                                    double totalReductionMs, double reductionPct, boolean meetsTarget,
                                    long targetMs, List<OptimizationStep> steps, List<String> tierRecommendations) {}
    public record StartupFleetSummary(double avgColdStartMs, double targetMetPct, long targetMs,
                                       Map<String, Double> avgByOem, Map<String, Long> devicesByTier,
                                       int totalDevices, long baselineProfileEnabled, long lazyInjectionEnabled,
                                       int slowDevices) {}
}
