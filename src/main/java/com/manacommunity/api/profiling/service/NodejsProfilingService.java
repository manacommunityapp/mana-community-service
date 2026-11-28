package com.manacommunity.api.profiling.service;

import com.manacommunity.api.profiling.entity.EventLoopSnapshot;
import com.manacommunity.api.profiling.repository.EventLoopSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NodejsProfilingService {

    private final EventLoopSnapshotRepository repo;

    public record EventLoopAnalysis(
            long totalSnapshots, long healthyCount, long laggingCount, long blockedCount, long criticalCount,
            double avgLagMs, double maxLagMs, double p99LagMs,
            long avgHeapUsedMb, long maxHeapUsedMb, long avgRssMb,
            String dominantBlockingCause, long totalBlockingDurationMs,
            int maxWorkerThreads, long maxJobQueueDepth,
            List<EventLoopSnapshot> recentSnapshots, List<EventLoopSnapshot> worstBlocks
    ) {}

    public EventLoopAnalysis getDashboard() {
        List<EventLoopSnapshot> all = repo.findAll();
        if (all.isEmpty()) {
            return new EventLoopAnalysis(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "N/A", 0, 0, 0, List.of(), List.of());
        }

        long healthy = all.stream().filter(s -> s.getHealth() == EventLoopSnapshot.LoopHealth.HEALTHY).count();
        long lagging = all.stream().filter(s -> s.getHealth() == EventLoopSnapshot.LoopHealth.LAGGING).count();
        long blocked = all.stream().filter(s -> s.getHealth() == EventLoopSnapshot.LoopHealth.BLOCKED).count();
        long critical = all.stream().filter(s -> s.getHealth() == EventLoopSnapshot.LoopHealth.CRITICAL).count();

        double avgLag = all.stream().mapToDouble(EventLoopSnapshot::getEventLoopLagMs).average().orElse(0);
        double maxLag = all.stream().mapToDouble(EventLoopSnapshot::getEventLoopLagMs).max().orElse(0);
        List<Double> sortedLags = all.stream().mapToDouble(EventLoopSnapshot::getEventLoopLagMs).sorted().boxed().toList();
        double p99 = sortedLags.get((int) (sortedLags.size() * 0.99));

        long avgHeap = (long) all.stream().mapToLong(EventLoopSnapshot::getHeapUsedMb).average().orElse(0);
        long maxHeap = all.stream().mapToLong(EventLoopSnapshot::getHeapUsedMb).max().orElse(0);
        long avgRss = (long) all.stream().mapToLong(EventLoopSnapshot::getRssMemoryMb).average().orElse(0);

        String dominantCause = all.stream()
                .filter(s -> s.getBlockingCause() != EventLoopSnapshot.BlockingCause.NONE)
                .collect(Collectors.groupingBy(EventLoopSnapshot::getBlockingCause, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name()).orElse("NONE");

        long totalBlocking = all.stream().mapToLong(EventLoopSnapshot::getBlockingDurationMs).sum();
        int maxWorkers = all.stream().mapToInt(EventLoopSnapshot::getWorkerThreadsActive).max().orElse(0);
        long maxQueue = all.stream().mapToLong(EventLoopSnapshot::getJobQueueDepth).max().orElse(0);

        List<EventLoopSnapshot> recent = all.stream()
                .sorted((a, b) -> b.getSnapshotTimestamp().compareTo(a.getSnapshotTimestamp()))
                .limit(10).toList();

        List<EventLoopSnapshot> worstBlocks = all.stream()
                .filter(s -> s.getBlockingDurationMs() > 0)
                .sorted((a, b) -> Long.compare(b.getBlockingDurationMs(), a.getBlockingDurationMs()))
                .limit(5).toList();

        return new EventLoopAnalysis(all.size(), healthy, lagging, blocked, critical,
                avgLag, maxLag, p99, avgHeap, maxHeap, avgRss, dominantCause, totalBlocking,
                maxWorkers, maxQueue, recent, worstBlocks);
    }
}
