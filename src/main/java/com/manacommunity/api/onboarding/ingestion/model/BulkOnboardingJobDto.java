package com.manacommunity.api.onboarding.ingestion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOnboardingJobDto {

    public enum Status {
        UPLOADED,
        PARSED,
        DRY_RUN_COMPLETED,
        COMMITTED,
        ROLLED_BACK,
        FAILED
    }

    private Long jobId;
    private Long communityId;
    private String fileName;
    private Status status;
    private int totalRows;
    private int validRows;
    private int errorRows;
    private int warningRows;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
