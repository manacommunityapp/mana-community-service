package com.manacommunity.api.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyVendorBillDto {
    private Long id;
    private String billNumber;
    private String vendorId;
    private String vendorName;
    private String billDate;
    private String dueDate;
    private String description;
    private BigDecimal grossAmount;
    private BigDecimal gstPercent;
    private BigDecimal gstAmount;
    private String tdsSection;
    private BigDecimal tdsAmount;
    private BigDecimal netPayable;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String status;
    private String approvedByName;
    private String approvedDate;
    private String approvalNotes;
    private String paymentMethod;
    private String utrReference;
    private String paidDate;
    private String glVoucherNumber;
    private String createdByName;
    private String createdAt;
}
