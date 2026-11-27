package com.manacommunity.api.db.optimization.mysql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxTaskDto {
    private Long id;
    private String taskType;
    private String payload;
    private TaskStatus status;
    private String lockedByWorker;
    private Instant scheduledAt;
    private Instant lockedAt;

    public enum TaskStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
