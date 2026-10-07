package com.manacommunity.api.transactioncore.dto;

import com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Transaction domain is required")
    private TransactionDomain domain;

    @NotBlank(message = "Order/Reference ID is required")
    private String orderReferenceId;

    private Long communityId;

    @NotNull(message = "Payer ID is required")
    private Long payerId;
    private String payerName;
    private String payerEmail;
    private String payerPhone;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    private PaymentRail preferredRail = PaymentRail.UPI;

    private BigDecimal walletDeductionAmount;

    @Builder.Default
    private boolean holdInEscrow = false;

    private EscrowReleaseCondition escrowReleaseCondition;

    private List<PaymentSplit> splits;
    private TaxBreakdownDto taxBreakdown;
    private Map<String, Object> metadata;
    private String remarks;
}
