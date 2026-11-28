package com.manacommunity.api.profiling.service;

import com.manacommunity.api.profiling.entity.BrowserMemorySnapshot;
import com.manacommunity.api.profiling.repository.BrowserMemorySnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrowserProfilingService {

    private final BrowserMemorySnapshotRepository repo;

    public record BrowserMemoryAnalysis(
            long totalSessions, long healthySessions, long growingSessions, long leakingSessions, long criticalSessions,
            long avgHeapUsedMb, long maxHeapUsedMb, long avgDomNodeCount, long maxDetachedDomNodes,
            long avgEventListeners, long maxEventListeners,
            long avgSessionDurationMin, long maxSessionDurationMin,
            double avgGrowthRateMbPerHr, double maxGrowthRateMbPerHr,
            String dominantLeakSource,
            long totalWebSocketLeaks, long totalQueryCacheLeaks, long totalMissingUnmounts,
            List<BrowserMemorySnapshot> recentSnapshots, List<BrowserMemorySnapshot> worstLeaks
    ) {}

    public BrowserMemoryAnalysis getDashboard() {
        List<BrowserMemorySnapshot> all = repo.findAll();
        if (all.isEmpty()) {
            return new BrowserMemoryAnalysis(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "N/A", 0, 0, 0, List.of(), List.of());
        }

        long healthy = all.stream().filter(s -> s.getHealth() == BrowserMemorySnapshot.MemoryHealth.HEALTHY).count();
        long growing = all.stream().filter(s -> s.getHealth() == BrowserMemorySnapshot.MemoryHealth.GROWING).count();
        long leaking = all.stream().filter(s -> s.getHealth() == BrowserMemorySnapshot.MemoryHealth.LEAKING).count();
        long critical = all.stream().filter(s -> s.getHealth() == BrowserMemorySnapshot.MemoryHealth.CRITICAL).count();

        long avgHeap = (long) all.stream().mapToLong(BrowserMemorySnapshot::getJsHeapUsedMb).average().orElse(0);
        long maxHeap = all.stream().mapToLong(BrowserMemorySnapshot::getJsHeapUsedMb).max().orElse(0);
        long avgDom = (long) all.stream().mapToLong(BrowserMemorySnapshot::getDomNodeCount).average().orElse(0);
        long maxDetached = all.stream().mapToLong(BrowserMemorySnapshot::getDetachedDomNodes).max().orElse(0);
        long avgListeners = (long) all.stream().mapToLong(BrowserMemorySnapshot::getEventListenerCount).average().orElse(0);
        long maxListeners = all.stream().mapToLong(BrowserMemorySnapshot::getEventListenerCount).max().orElse(0);

        long avgSession = (long) all.stream().mapToLong(BrowserMemorySnapshot::getSessionDurationMinutes).average().orElse(0);
        long maxSession = all.stream().mapToLong(BrowserMemorySnapshot::getSessionDurationMinutes).max().orElse(0);

        double avgGrowth = all.stream().mapToLong(BrowserMemorySnapshot::getMemoryGrowthRateMbPerHour).average().orElse(0);
        double maxGrowth = all.stream().mapToLong(BrowserMemorySnapshot::getMemoryGrowthRateMbPerHour).max().orElse(0);

        String dominantLeak = all.stream()
                .filter(s -> s.getPrimaryLeakSource() != BrowserMemorySnapshot.LeakSource.NONE)
                .collect(Collectors.groupingBy(BrowserMemorySnapshot::getPrimaryLeakSource, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name()).orElse("NONE");

        long wsLeaks = all.stream().filter(s -> !s.isWebsocketListenersCleaned()).count();
        long cacheLeaks = all.stream().mapToLong(BrowserMemorySnapshot::getReactQueryStaleCacheEntries).sum();
        long missingUnmounts = all.stream().mapToLong(BrowserMemorySnapshot::getReactComponentUnmountMissing).sum();

        List<BrowserMemorySnapshot> recent = all.stream()
                .sorted((a, b) -> b.getSnapshotTimestamp().compareTo(a.getSnapshotTimestamp()))
                .limit(10).toList();

        List<BrowserMemorySnapshot> worstLeaks = all.stream()
                .sorted((a, b) -> Long.compare(b.getMemoryGrowthRateMbPerHour(), a.getMemoryGrowthRateMbPerHour()))
                .limit(5).toList();

        return new BrowserMemoryAnalysis(all.size(), healthy, growing, leaking, critical,
                avgHeap, maxHeap, avgDom, maxDetached, avgListeners, maxListeners,
                avgSession, maxSession, avgGrowth, maxGrowth, dominantLeak,
                wsLeaks, cacheLeaks, missingUnmounts, recent, worstLeaks);
    }
}
