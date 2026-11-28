package com.manacommunity.api.payments.reconciliation.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single transaction row in the bank settlement file (T+1 MIS / MT940 / CSV).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankSettlementRecord {
    private String settlementId;
    private String utr;
    private String gatewayPaymentId;
    private String invoiceReference;
    private String payerName;
    private LocalDate settlementDate;
    private BigDecimal grossAmount;
    private BigDecimal mdrFee;
    private BigDecimal gstOnMdr;
    private BigDecimal netSettledAmount;
    private String narration;
    private String bankAccount;
}
