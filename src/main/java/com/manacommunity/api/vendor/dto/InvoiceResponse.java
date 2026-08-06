package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private VendorRef vendor;
    private String status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String notes;
    private List<InvoiceItemResponse> items;
    private Long communityId;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class VendorRef {
        private Long id;
        private String businessName;
    }

    @Data
    @Builder
    public static class InvoiceItemResponse {
        private Long id;
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxPercent;
        private BigDecimal discountPercent;
        private BigDecimal total;
    }
}
