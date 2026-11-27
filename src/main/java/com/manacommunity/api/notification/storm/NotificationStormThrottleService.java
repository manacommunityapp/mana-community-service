package com.manacommunity.api.notification.storm;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Priority-laned notification storm controller preventing society-wide broadcasts
 * (e.g. 10,000-resident water shutdown or fire drill) from exhausting FCM/SMS/IVR
 * quotas or starving live visitor gate approvals.
 */
@Slf4j
@Service
public class NotificationStormThrottleService {

    public enum PriorityLane {
        CRITICAL_REALTIME, // Live visitor approvals, gate OTPs, SOS triggers
        BULK_BROADCAST     // 10k-resident announcements, newsletters
    }

    public static final int FCM_MULTICAST_BATCH_SIZE = 500;
    private static final double CRITICAL_HEADROOM_RATIO = 0.30; // Reserve 30% TPS exclusively for critical lane

    private final Executor criticalExecutor;
    private final Executor bulkBroadcastExecutor;
    private final Clock clock;
    private final int totalProviderCapacityPerSec;

    private double availableTokens;
    private long lastRefillMillis;

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationStormThrottleService(
            @Qualifier("notificationExecutor") Executor criticalExecutor,
            @Qualifier("broadcastNotificationExecutor") Executor bulkBroadcastExecutor) {
        this(criticalExecutor, bulkBroadcastExecutor, Clock.systemUTC(), 100);
    }

    public NotificationStormThrottleService(
            Executor criticalExecutor,
            Executor bulkBroadcastExecutor,
            Clock clock,
            int totalProviderCapacityPerSec) {
        this.criticalExecutor = criticalExecutor;
        this.bulkBroadcastExecutor = bulkBroadcastExecutor;
        this.clock = clock;
        this.totalProviderCapacityPerSec = Math.max(10, totalProviderCapacityPerSec);
        this.availableTokens = this.totalProviderCapacityPerSec;
        this.lastRefillMillis = clock.millis();
    }

    /**
     * Enqueues a critical real-time visitor approval/OTP on the dedicated high-priority lane.
     * Bypasses bulk broadcast caps using the reserved 30% headroom.
     */
    public boolean dispatchCritical(Runnable task) {
        if (!tryAcquireToken(PriorityLane.CRITICAL_REALTIME)) {
            log.warn("[NotificationStorm] Critical lane hit absolute ceiling; executing best-effort priority push.");
        }
        criticalExecutor.execute(task);
        return true;
    }

    /**
     * Expands a large resident audience into 500-token multicast chunks and dispatches
     * them through the bulk token bucket on the isolated low-priority executor.
     */
    public BroadcastDispatchPlan scheduleBulkBroadcast(
            List<Long> residentIds,
            Consumer<List<Long>> batchDispatcher) {

        List<List<Long>> chunks = partition(residentIds, FCM_MULTICAST_BATCH_SIZE);
        int dispatchedImmediate = 0;
        int pacedOrDeferred = 0;

        for (List<Long> chunk : chunks) {
            if (tryAcquireToken(PriorityLane.BULK_BROADCAST)) {
                dispatchedImmediate++;
                bulkBroadcastExecutor.execute(() -> batchDispatcher.accept(chunk));
            } else {
                pacedOrDeferred++;
            }
        }

        return BroadcastDispatchPlan.builder()
                .totalRecipients(residentIds != null ? residentIds.size() : 0)
                .totalChunks(chunks.size())
                .dispatchedChunks(dispatchedImmediate)
                .pacedChunks(pacedOrDeferred)
                .reservedCriticalTokens(reservedCriticalFloor())
                .build();
    }

    public synchronized boolean tryAcquireToken(PriorityLane lane) {
        refill();
        double reservedFloor = reservedCriticalFloor();

        if (lane == PriorityLane.CRITICAL_REALTIME) {
            if (availableTokens >= 1.0) {
                availableTokens -= 1.0;
                return true;
            }
            return false;
        }

        // Bulk traffic can NEVER consume below the 30% critical headroom floor
        if (availableTokens - 1.0 >= reservedFloor) {
            availableTokens -= 1.0;
            return true;
        }
        return false;
    }

    public synchronized double getAvailableTokens() {
        refill();
        return availableTokens;
    }

    private double reservedCriticalFloor() {
        return Math.ceil(totalProviderCapacityPerSec * CRITICAL_HEADROOM_RATIO);
    }

    private void refill() {
        long now = clock.millis();
        long elapsedMs = Math.max(0L, now - lastRefillMillis);
        if (elapsedMs > 0) {
            double added = (elapsedMs / 1000.0) * totalProviderCapacityPerSec;
            availableTokens = Math.min(totalProviderCapacityPerSec, availableTokens + added);
            lastRefillMillis = now;
        }
    }

    public static <T> List<List<T>> partition(List<T> items, int chunkSize) {
        if (items == null || items.isEmpty()) return List.of();
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += chunkSize) {
            chunks.add(items.subList(i, Math.min(items.size(), i + chunkSize)));
        }
        return chunks;
    }

    @Data
    @Builder
    public static class BroadcastDispatchPlan {
        private int totalRecipients;
        private int totalChunks;
        private int dispatchedChunks;
        private int pacedChunks;
        private double reservedCriticalTokens;
    }
}
