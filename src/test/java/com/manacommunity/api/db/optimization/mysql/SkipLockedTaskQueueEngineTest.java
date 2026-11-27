package com.manacommunity.api.db.optimization.mysql;

import com.manacommunity.api.db.optimization.mysql.model.OutboxTaskDto;
import com.manacommunity.api.db.optimization.mysql.service.SkipLockedTaskQueueEngine;
import com.manacommunity.api.db.optimization.mysql.service.SkipLockedTaskQueueEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkipLockedTaskQueueEngineTest {

    private SkipLockedTaskQueueEngine queueEngine;

    @BeforeEach
    void setUp() {
        queueEngine = new SkipLockedTaskQueueEngineImpl();
        // Enqueue 100 outbox tasks
        for (int i = 1; i <= 100; i++) {
            queueEngine.enqueueTask("PUSH_NOTIFICATION", "{\"residentId\":" + i + ",\"body\":\"Visitor at Gate\"}");
        }
    }

    @Test
    @DisplayName("Concurrent worker threads poll with SKIP LOCKED acquiring 100% disjoint batches with zero contention")
    void testConcurrentSkipLockedWorkers() throws InterruptedException, ExecutionException {
        int workerCount = 10;
        int batchSize = 10;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<Callable<List<OutboxTaskDto>>> tasks = new ArrayList<>();

        // 10 concurrent workers polling simultaneously
        for (int i = 1; i <= workerCount; i++) {
            final String workerId = "worker-node-" + i;
            tasks.add(() -> queueEngine.pollAndLockBatch(workerId, batchSize));
        }

        List<Future<List<OutboxTaskDto>>> futures = executor.invokeAll(tasks);
        Set<Long> lockedTaskIds = ConcurrentHashMap.newKeySet();
        int totalAcquired = 0;

        for (Future<List<OutboxTaskDto>> future : futures) {
            List<OutboxTaskDto> batch = future.get();
            assertEquals(batchSize, batch.size(), "Each worker should acquire exactly 10 tasks");
            totalAcquired += batch.size();

            for (OutboxTaskDto task : batch) {
                assertEquals(OutboxTaskDto.TaskStatus.PROCESSING, task.getStatus());
                boolean isUnique = lockedTaskIds.add(task.getId());
                assertEquals(true, isUnique, "Task ID " + task.getId() + " was acquired by multiple workers!");
            }
        }

        assertEquals(100, totalAcquired, "All 100 tasks should have been acquired across workers");
        assertEquals(100, lockedTaskIds.size(), "All acquired task IDs must be strictly unique (disjoint sets)");
        assertEquals(0, queueEngine.getPendingTaskCount(), "Zero pending tasks should remain");

        executor.shutdown();
    }
}
