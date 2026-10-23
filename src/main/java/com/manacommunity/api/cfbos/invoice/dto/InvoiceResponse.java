package com.manacommunity.api.cfbos.invoice.dto;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceStatus;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private Long propertyId;
    private Long residentId;
    private Long billingRunId;
    private InvoiceType invoiceType;
    private InvoiceStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalTax;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String notes;
    private String terms;
    private Boolean isGstInvoice;
    private String communityGstin;
    private Long journalEntryId;
    private List<InvoiceLineDto> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InvoiceLineDto {
        private Long id;
        private Long chargeTypeId;
        private String description;
        private String hsnSacCode;
        private BigDecimal quantity;
        private BigDecimal rate;
        private BigDecimal amount;
        private BigDecimal discountAmount;
        private BigDecimal taxableAmount;
        private BigDecimal cgstRate;
        private BigDecimal cgstAmount;
        private BigDecimal sgstRate;
        private BigDecimal sgstAmount;
        private BigDecimal igstRate;
        private BigDecimal igstAmount;
        private BigDecimal totalAmount;
    }
}
