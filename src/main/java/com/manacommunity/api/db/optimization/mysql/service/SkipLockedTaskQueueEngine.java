package com.manacommunity.api.db.optimization.mysql.service;

import com.manacommunity.api.db.optimization.mysql.model.OutboxTaskDto;

import java.util.List;

public interface SkipLockedTaskQueueEngine {

    /**
     * Enqueues an outbox task into the queue.
     */
    OutboxTaskDto enqueueTask(String taskType, String payload);

    /**
     * Polls and locks a batch of tasks simulating MySQL 8.0:
     * SELECT * FROM outbox_tasks 
     * WHERE status = 'PENDING' 
     * ORDER BY scheduled_at ASC 
     * LIMIT :batchSize 
     * FOR UPDATE SKIP LOCKED
     *
     * Multiple concurrent threads calling this method will immediately acquire
     * mutually exclusive, non-overlapping subsets of pending tasks with zero lock wait.
     */
    List<OutboxTaskDto> pollAndLockBatch(String workerId, int batchSize);

    /**
     * Marks a processed task as COMPLETED and releases locks.
     */
    void completeTask(Long taskId, String workerId);

    /**
     * Returns the total count of pending tasks remaining in the queue.
     */
    int getPendingTaskCount();
}
