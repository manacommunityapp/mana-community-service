package com.manacommunity.api.fresh.vendor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveCapabilityRequest {

    @NotNull(message = "Approved flag is required")
    private Boolean approved;

    private String rejectionReason;
}
