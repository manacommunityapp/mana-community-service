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
public class ManaDigitalReceiptDto {
    private String receiptNumber;
    private LocalDateTime receiptDate;
    private String transactionNumber;
    private String orderReferenceId;
    private TransactionDomain domain;
    private TransactionIntentStatus status;
    private String payerName;
    private String payerUnit;
    private String payerPhone;
    private PaymentRail paymentRail;
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal convenienceFee;
    private BigDecimal totalAmount;
    private String utr;
    private String gatewayRef;
    private String societyName;
    private String societyGstin;
    private String societyAddress;
    private List<Map<String, Object>> items;
    private String notes;
    private boolean verifiedStamp;
}
