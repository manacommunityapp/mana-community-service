package com.manacommunity.api.safety.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GuardShiftResponse {
    private Long id;
    private Long guardId;
    private String guardName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String area;
    private String status;
    private Integer checkpointsCompleted;
    private Integer totalCheckpoints;
    private LocalDateTime createdAt;
}
