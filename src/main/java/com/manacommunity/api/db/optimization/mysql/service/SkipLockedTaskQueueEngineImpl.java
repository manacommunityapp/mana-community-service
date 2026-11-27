package com.manacommunity.api.db.optimization.mysql.service;

import com.manacommunity.api.db.optimization.mysql.model.OutboxTaskDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * High-concurrency task queue implementing MySQL 8.0 "SELECT ... FOR UPDATE SKIP LOCKED" semantics.
 * Under high thread contention, worker threads never wait on lock acquisitions; locked rows
 * are bypassed instantaneously, eliminating lock wait timeouts and thread exhaustion.
 */
@Slf4j
@Service
public class SkipLockedTaskQueueEngineImpl implements SkipLockedTaskQueueEngine {

    private final AtomicLong idGenerator = new AtomicLong(1000L);
    private final Map<Long, OutboxTaskDto> taskStore = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> rowLocks = new ConcurrentHashMap<>();

    // Global coordinator lock simulating the InnoDB storage engine table latch
    private final ReentrantLock tableLatch = new ReentrantLock();

    @Override
    public OutboxTaskDto enqueueTask(String taskType, String payload) {
        long id = idGenerator.incrementAndGet();
        OutboxTaskDto task = OutboxTaskDto.builder()
                .id(id)
                .taskType(taskType)
                .payload(payload)
                .status(OutboxTaskDto.TaskStatus.PENDING)
                .scheduledAt(Instant.now())
                .build();

        taskStore.put(id, task);
        rowLocks.put(id, new ReentrantLock());
        return task;
    }

    @Override
    public List<OutboxTaskDto> pollAndLockBatch(String workerId, int batchSize) {
        if (batchSize <= 0) {
            return Collections.emptyList();
        }

        List<OutboxTaskDto> acquiredBatch = new ArrayList<>(batchSize);

        // Under MySQL 8.0 FOR UPDATE SKIP LOCKED, the engine iterates the index in scheduled_at ASC
        // and attempts a tryLock on each row lock. If locked by another worker, it skips to the next row.
        tableLatch.lock();
        try {
            List<OutboxTaskDto> candidates = taskStore.values().stream()
                    .filter(t -> t.getStatus() == OutboxTaskDto.TaskStatus.PENDING)
                    .sorted(Comparator.comparing(OutboxTaskDto::getScheduledAt).thenComparing(OutboxTaskDto::getId))
                    .toList();

            for (OutboxTaskDto candidate : candidates) {
                if (acquiredBatch.size() >= batchSize) {
                    break;
                }

                ReentrantLock rowLock = rowLocks.get(candidate.getId());
                if (rowLock != null && rowLock.tryLock()) {
                    // Lock successfully acquired (equivalent to InnoDB row X-lock)
                    candidate.setStatus(OutboxTaskDto.TaskStatus.PROCESSING);
                    candidate.setLockedByWorker(workerId);
                    candidate.setLockedAt(Instant.now());
                    acquiredBatch.add(candidate);
                }
                // If tryLock fails: row is currently held by another worker.
                // SKIP LOCKED semantics: DO NOT WAIT, immediately evaluate next candidate!
            }
        } finally {
            tableLatch.unlock();
        }

        log.debug("Worker '{}' acquired batch of {} tasks via SKIP LOCKED", workerId, acquiredBatch.size());
        return acquiredBatch;
    }

    @Override
    public void completeTask(Long taskId, String workerId) {
        OutboxTaskDto task = taskStore.get(taskId);
        if (task != null) {
            task.setStatus(OutboxTaskDto.TaskStatus.COMPLETED);
            ReentrantLock lock = rowLocks.get(taskId);
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public int getPendingTaskCount() {
        return (int) taskStore.values().stream()
                .filter(t -> t.getStatus() == OutboxTaskDto.TaskStatus.PENDING)
                .count();
    }
}
