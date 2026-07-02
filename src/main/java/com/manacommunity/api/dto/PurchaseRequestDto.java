package com.manacommunity.api.dto;

import com.manacommunity.api.model.ExpenseCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseRequestDto {
    private String title;
    private String description;
    private ExpenseCategory category;
    private BigDecimal estimatedAmount;
    private Long selectedVendorId;
    private String requestedBy;
    private LocalDate neededBy;
    private String approvalNotes;
    private String purchaseOrderNumber;
}
