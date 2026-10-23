package com.manacommunity.api.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSplitDetail {
    @Builder.Default
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal platformFeeTax = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal vendorPayout = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
}
