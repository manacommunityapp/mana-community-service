package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KarateBatchStatusRequest {

    @NotBlank
    private String status;   // UPCOMING, ACTIVE, COMPLETED, CANCELLED
}
