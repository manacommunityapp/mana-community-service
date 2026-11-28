package com.manacommunity.api.profiling.service;

import com.manacommunity.api.profiling.entity.GcEvent;
import com.manacommunity.api.profiling.entity.ThreadPoolSnapshot;
import com.manacommunity.api.profiling.repository.GcEventRepository;
import com.manacommunity.api.profiling.repository.ThreadPoolSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JvmProfilingService {

    private final GcEventRepository gcRepo;
    private final ThreadPoolSnapshotRepository threadRepo;

    public record GcAnalysis(
            long totalGcEvents, long stwPauses, double avgPauseMs, long maxPauseMs,
            double p99PauseMs, long totalPauseMs, double gcOverheadPercent,
            String dominantCollector, String dominantCause,
            List<GcEvent> worstPauses, List<GcEvent> recentEvents
    ) {}

    public record ThreadPoolAnalysis(
            int totalPools, int healthyPools, int warningPools, int criticalPools, int starvedPools,
            long totalActiveThreads, long totalQueuedTasks, long totalRejectedTasks,
            boolean virtualThreadsAvailable, int virtualThreadCount,
            List<ThreadPoolSnapshot> snapshots
    ) {}

    public record JvmDashboard(GcAnalysis gcAnalysis, ThreadPoolAnalysis threadAnalysis) {}

    public JvmDashboard getDashboard() {
        List<GcEvent> allGc = gcRepo.findAll();
        List<ThreadPoolSnapshot> allThreads = threadRepo.findAll();

        GcAnalysis gcAnalysis = analyzeGc(allGc);
        ThreadPoolAnalysis threadAnalysis = analyzeThreads(allThreads);

        return new JvmDashboard(gcAnalysis, threadAnalysis);
    }

    private GcAnalysis analyzeGc(List<GcEvent> events) {
        if (events.isEmpty()) {
            return new GcAnalysis(0, 0, 0, 0, 0, 0, 0, "N/A", "N/A", List.of(), List.of());
        }

        long stwCount = events.stream().filter(GcEvent::isStopTheWorld).count();
        double avgPause = events.stream().mapToLong(GcEvent::getPauseMillis).average().orElse(0);
        long maxPause = events.stream().mapToLong(GcEvent::getPauseMillis).max().orElse(0);
        long totalPause = events.stream().mapToLong(GcEvent::getPauseMillis).sum();

        List<Long> sorted = events.stream().mapToLong(GcEvent::getPauseMillis).sorted().boxed().toList();
        long p99 = sorted.get((int) (sorted.size() * 0.99));

        String dominantCollector = events.stream()
                .collect(Collectors.groupingBy(GcEvent::getCollector, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name()).orElse("N/A");

        String dominantCause = events.stream()
                .collect(Collectors.groupingBy(GcEvent::getCause, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name()).orElse("N/A");

        List<GcEvent> worstPauses = events.stream()
                .sorted((a, b) -> Long.compare(b.getPauseMillis(), a.getPauseMillis()))
                .limit(5).toList();

        List<GcEvent> recent = events.stream()
                .sorted((a, b) -> b.getEventTimestamp().compareTo(a.getEventTimestamp()))
                .limit(10).toList();

        return new GcAnalysis(events.size(), stwCount, avgPause, maxPause, p99, totalPause,
                (totalPause * 100.0) / (3600_000), dominantCollector, dominantCause, worstPauses, recent);
    }

    private ThreadPoolAnalysis analyzeThreads(List<ThreadPoolSnapshot> snapshots) {
        int healthy = (int) snapshots.stream().filter(s -> s.getHealth() == ThreadPoolSnapshot.PoolHealth.HEALTHY).count();
        int warning = (int) snapshots.stream().filter(s -> s.getHealth() == ThreadPoolSnapshot.PoolHealth.WARNING).count();
        int critical = (int) snapshots.stream().filter(s -> s.getHealth() == ThreadPoolSnapshot.PoolHealth.CRITICAL).count();
        int starved = (int) snapshots.stream().filter(s -> s.getHealth() == ThreadPoolSnapshot.PoolHealth.STARVED).count();

        long activeThreads = snapshots.stream().mapToInt(ThreadPoolSnapshot::getActiveThreads).sum();
        long queuedTasks = snapshots.stream().mapToInt(ThreadPoolSnapshot::getQueuedTasks).sum();
        long rejectedTasks = snapshots.stream().mapToLong(ThreadPoolSnapshot::getRejectedTasks).sum();

        boolean virtualAvailable = snapshots.stream().anyMatch(ThreadPoolSnapshot::isVirtualThreadsEnabled);
        int virtualCount = snapshots.stream().mapToInt(ThreadPoolSnapshot::getVirtualThreadCount).sum();

        return new ThreadPoolAnalysis(snapshots.size(), healthy, warning, critical, starved,
                activeThreads, queuedTasks, rejectedTasks, virtualAvailable, virtualCount, snapshots);
    }
}
