package com.manacommunity.api.safety.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatrolScanRequest {

    @NotNull
    private Long checkpointId;

    private Long shiftId;
    private String notes;
}
