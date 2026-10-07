package com.manacommunity.api.transactioncore.dto;

import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {

    private String intentId;
    private String idempotencyKey;
    private TransactionDomain domain;
    private String orderReferenceId;
    private Long communityId;
    private Long payerId;
    private BigDecimal amount;
    private String currency;
    private TransactionIntentStatus status;
    private PaymentRail paymentRail;
    private String gatewayPaymentUrl;
    private String gatewayOrderId;
    private String escrowId;
    private boolean escrowHeld;
    private List<PaymentSplit> splits;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
