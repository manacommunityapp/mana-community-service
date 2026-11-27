package com.manacommunity.api.db.optimization.erp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRecordDto {

    private Long invoiceId;
    private Long communityId;
    private Long flatId;
    private String unitNumber;
    private String residentName;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String status; // PENDING, PARTIALLY_PAID, PAID
}
