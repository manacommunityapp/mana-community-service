package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashfreeRefundRequest {

    /** Amount to refund in INR. If null, the full order amount is refunded. */
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal amount;

    /** Optional human-readable reason shown in the Cashfree dashboard. */
    private String reason;
}
