package com.manacommunity.api.transactioncore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSplit {
    private String recipientType;
    private Long recipientId;
    private String recipientAccount;
    private BigDecimal amount;
    private BigDecimal commissionAmount;
    private BigDecimal tdsAmount;
    private BigDecimal netSettlementAmount;
    private String narration;
}
