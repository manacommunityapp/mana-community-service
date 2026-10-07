package com.manacommunity.api.transactioncore.dto;

import com.manacommunity.api.transactioncore.enums.RefundDestination;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotBlank(message = "Intent ID is required")
    private String intentId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be positive")
    private BigDecimal refundAmount;

    @Builder.Default
    private RefundDestination destination = RefundDestination.WALLET_INSTANT;

    @NotBlank(message = "Refund reason is required")
    private String reason;

    private String requestedBy;
}
