package com.manacommunity.api.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyExpenseDto {
    private Long id;
    private String voucherNumber;
    private String category;
    private String title;
    private String description;
    private BigDecimal amount;
    private String accountId;
    private String accountName;
    private String vendorId;
    private String vendorName;
    private String status;
    private String makerName;
    private String makerDate;
    private String checkerName;
    private String checkerDate;
    private String checkerNotes;
    private String approverName;
    private String approverDate;
    private String approverNotes;
    private String receiptUrl;
    private String paymentMethod;
    private String utrReference;
    private String createdAt;
}
