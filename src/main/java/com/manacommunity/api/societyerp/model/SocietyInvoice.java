package com.manacommunity.api.societyerp.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintenance invoice generated for a flat with itemized heads,
 * statutory GST breakdown, late penalties, advance wallet deductions, and UPI intent URI.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyInvoice {

    public enum InvoiceStatus {
        DRAFT,
        PUBLISHED,
        PAID_VIA_WALLET,
        PARTIALLY_PAID_VIA_WALLET,
        PAID,
        OVERDUE
    }

    private String invoiceId;
    private String societyId;
    private String societyName;
    private String flatNumber;
    private String tower;
    private String residentId;
    private String residentName;
    private String billingPeriod; // "2026-11"

    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private LocalDate graceDate;

    @Builder.Default
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    // Subtotals
    private BigDecimal taxableSubtotal;
    private BigDecimal nonTaxableSubtotal;

    // GST
    private boolean gstApplicable;
    private BigDecimal cgstRatePct;
    private BigDecimal cgstAmount;
    private BigDecimal sgstRatePct;
    private BigDecimal sgstAmount;
    private BigDecimal totalGstAmount;

    // Arrears & Penalties
    private BigDecimal arrearsAmount;
    private BigDecimal latePenaltyAmount;

    // Gross vs Deductions
    private BigDecimal grossTotalAmount;
    private BigDecimal advanceWalletDeduction;
    private BigDecimal netPayableAmount;

    private InvoiceStatus status;
    private String upiIntentUri;

    // Settlement metadata
    private String settledPaymentId;
    private String settledPaymentMethod;
    private String settledUtr;
    private Instant settledAt;
    private String receiptNumber;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceLineItem {
        private String headName;
        private BigDecimal amount;
        private boolean taxable;
        private String description;
    }
}
