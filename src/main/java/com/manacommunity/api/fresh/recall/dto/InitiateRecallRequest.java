package com.manacommunity.api.fresh.recall.dto;

import com.manacommunity.api.fresh.recall.model.RecallSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateRecallRequest {

    @NotNull(message = "Vendor Product ID is required")
    private Long vendorProductId;

    private Long batchId;

    @NotNull(message = "Severity is required")
    private RecallSeverity severity;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String detailedRootCause;

    @NotNull(message = "Initiating user ID is required")
    private Long initiatedByUserId;
}
