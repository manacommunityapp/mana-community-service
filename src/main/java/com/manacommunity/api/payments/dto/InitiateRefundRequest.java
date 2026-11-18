package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiateRefundRequest {
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "refund amount must be positive")
    private BigDecimal amount;

    private String reason;
}
