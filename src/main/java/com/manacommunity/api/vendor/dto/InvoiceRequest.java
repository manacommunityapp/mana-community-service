package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequest {
    @NotNull
    private Long vendorId;
    private Long bookingId;
    private Long contractId;
    @NotNull
    private LocalDate dueDate;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String notes;
    @NotNull
    private List<InvoiceItemRequest> items;

    @Data
    public static class InvoiceItemRequest {
        private String description;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxPercent;
        private BigDecimal discountPercent;
    }
}
