package com.manacommunity.api.safety.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PatrolLogResponse {
    private Long id;
    private String guardName;
    private String checkpointName;
    private String checkpointLocation;
    private LocalDateTime scannedAt;
    private String notes;
    private Long shiftId;
    private LocalDateTime createdAt;
}
