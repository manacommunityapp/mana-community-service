package com.manacommunity.api.transaction.model;

import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionIntent {
    private TransactionDomain domain;
    private Long payerId;
    private Long payeeId;
    private Long communityId;
    private Long propertyId;

    private BigDecimal amount;
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;
    @Builder.Default
    private String currency = "INR";

    private TransactionPaymentMethod paymentMethod;
    private String gatewayReference;
    private String gatewayPaymentId;
    private String idempotencyKey;

    private String referenceType;
    private String referenceId;
    private String narration;

    @Builder.Default
    private boolean isEscrowRequired = false;
    private Integer escrowAutoReleaseMinutes;

    @Builder.Default
    private List<TransactionLineItem> lineItems = new ArrayList<>();
}
