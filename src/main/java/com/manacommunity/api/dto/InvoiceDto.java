package com.manacommunity.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class InvoiceDto {
    private String invoiceNumber;
    private String invoiceDate;
    private Long vendorId;
    private String vendorName;
    private String vendorGstNumber;
    private BigDecimal totalAmount;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private String expenseCategory;
    private String uploadedBy;
    private String receiptUrl;
    private String upiId;
    private String bankDetails;
    private LocalDate dueDate;
    private Long purchaseRequestId;
    private String purchaseOrderNumber;
    private Map<String, Object> ocrRawMetadata;
    private List<LineItemDto> lineItems;
    private Long assetId; // Optional link to existing asset
}
