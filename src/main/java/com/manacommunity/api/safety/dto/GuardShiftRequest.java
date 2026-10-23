package com.manacommunity.api.safety.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GuardShiftRequest {

    @NotNull
    private Long guardId;

    @NotNull
    private LocalDateTime startTime;

    private LocalDateTime endTime;
    private String area;
    private Integer totalCheckpoints;
}
