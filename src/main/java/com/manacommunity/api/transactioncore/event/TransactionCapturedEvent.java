package com.manacommunity.api.transactioncore.event;

import com.manacommunity.api.transactioncore.entity.TransactionIntent;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCapturedEvent {
    private String intentId;
    private String orderReferenceId;
    private TransactionDomain domain;
    private Long payerId;
    private String payerName;
    private String payerPhone;
    private String payerUnit;
    private BigDecimal amount;
    private PaymentRail paymentRail;
    private String utr;
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String receiptNumber;
    private LocalDateTime capturedAt;
    private TransactionIntent intent;
}
