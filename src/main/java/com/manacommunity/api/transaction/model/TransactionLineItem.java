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
public class TransactionLineItem {
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private String hsnSacCode;
    private String accountCode;
}
