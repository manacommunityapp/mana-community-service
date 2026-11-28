package com.manacommunity.api.societyerp;

import com.manacommunity.api.societyerp.model.FlatBillingProfile;
import com.manacommunity.api.societyerp.model.MaintenanceBillingConfig;
import com.manacommunity.api.societyerp.model.PaymentReceipt;
import com.manacommunity.api.societyerp.model.SocietyInvoice;
import com.manacommunity.api.societyerp.model.SocietyInvoice.InvoiceStatus;
import com.manacommunity.api.societyerp.service.SocietyBillingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SocietyBillingEngineTest {

    private SocietyBillingEngine engine;
    private MaintenanceBillingConfig standardConfig;

    @BeforeEach
    void setUp() {
        engine = new SocietyBillingEngine();
        standardConfig = MaintenanceBillingConfig.builder()
                .societyId("SOC-PALM-01")
                .societyName("Palm Springs RWA")
                .societyGstin("29AAAAA0000A1Z5")
                .vpaAddress("palmsprings.rwa@icici")
                .baseRatePerSqFt(new BigDecimal("3.50"))
                .fixedSinkingFund(new BigDecimal("500.00"))
                .parkingSlotRate(new BigDecimal("300.00"))
                .waterRatePerUnit(new BigDecimal("45.00"))
                .gstExemptionThreshold(new BigDecimal("7500.00"))
                .gstRatePct(new BigDecimal("18.00"))
                .societyTurnoverAbove20Lakhs(true)
                .dueDayOffset(10)
                .graceDays(5)
                .latePenaltyAnnualInterestPct(new BigDecimal("18.00"))
                .build();
    }

    @Test
    void testGstExemptWhenMonthlyMaintenanceIsBelowSevenThousandFiveHundred() {
        // Flat 1200 sq ft @ 3.50 = 4200. Sinking = 500. 1 Parking = 300. Taxable = 5000 (< 7500 threshold)
        // Water = 10 KL @ 45 = 450 (non-taxable)
        FlatBillingProfile flat = FlatBillingProfile.builder()
                .flatNumber("A-201")
                .tower("Tower A")
                .carpetAreaSqFt(1200.0)
                .residentId("RES-101")
                .residentName("Sunil Sharma")
                .parkingSlotCount(1)
                .waterUnitsConsumed(10.0)
                .advanceWalletBalance(BigDecimal.ZERO)
                .build();

        SocietyInvoice invoice = engine.generateInvoice(standardConfig, flat, LocalDate.of(2026, 11, 1));

        assertThat(invoice.getTaxableSubtotal()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(invoice.getNonTaxableSubtotal()).isEqualByComparingTo(new BigDecimal("450.00"));

        // GST must be EXEMPT (0%)
        assertThat(invoice.isGstApplicable()).isFalse();
        assertThat(invoice.getTotalGstAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getCgstAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getSgstAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // Gross = 5000 + 450 = 5450.00
        assertThat(invoice.getGrossTotalAmount()).isEqualByComparingTo(new BigDecimal("5450.00"));
        assertThat(invoice.getNetPayableAmount()).isEqualByComparingTo(new BigDecimal("5450.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PUBLISHED);

        // Dynamic UPI Link generated
        assertThat(invoice.getUpiIntentUri())
                .contains("upi://pay?")
                .contains("pa=palmsprings.rwa@icici")
                .contains("am=5450.00")
                .contains("tr=INV-SOC-PALM-01-202611-A-201");
    }

    @Test
    void testGstApplicableAtEighteenPercentWhenMonthlyMaintenanceExceedsSevenThousandFiveHundred() {
        // Penthouse 2400 sq ft @ 3.50 = 8400. Sinking = 500. 2 Parking = 600. Taxable = 9500 (> 7500 threshold)
        FlatBillingProfile flat = FlatBillingProfile.builder()
                .flatNumber("PH-1401")
                .tower("Tower B")
                .carpetAreaSqFt(2400.0)
                .residentId("RES-901")
                .residentName("Ramesh Singhania")
                .parkingSlotCount(2)
                .waterUnitsConsumed(0.0)
                .advanceWalletBalance(BigDecimal.ZERO)
                .build();

        SocietyInvoice invoice = engine.generateInvoice(standardConfig, flat, LocalDate.of(2026, 11, 1));

        assertThat(invoice.getTaxableSubtotal()).isEqualByComparingTo(new BigDecimal("9500.00"));

        // GST is APPLICABLE at 18% (9% CGST + 9% SGST)
        assertThat(invoice.isGstApplicable()).isTrue();
        assertThat(invoice.getCgstAmount()).isEqualByComparingTo(new BigDecimal("855.00")); // 9% of 9500
        assertThat(invoice.getSgstAmount()).isEqualByComparingTo(new BigDecimal("855.00")); // 9% of 9500
        assertThat(invoice.getTotalGstAmount()).isEqualByComparingTo(new BigDecimal("1710.00")); // 18% of 9500

        // Gross = 9500 + 1710 = 11210.00
        assertThat(invoice.getGrossTotalAmount()).isEqualByComparingTo(new BigDecimal("11210.00"));
        assertThat(invoice.getNetPayableAmount()).isEqualByComparingTo(new BigDecimal("11210.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PUBLISHED);
    }

    @Test
    void testAdvanceWalletFullOffsetMarksInvoicePaidAndIssuesInstantReceipt() {
        // Bill = ₹5,450.00, Resident Advance Wallet = ₹6,500.00 (e.g. from previous delayed UPI webhook)
        FlatBillingProfile flat = FlatBillingProfile.builder()
                .flatNumber("C-402")
                .tower("Tower C")
                .carpetAreaSqFt(1200.0) // 4200
                .parkingSlotCount(1)    // 300
                .waterUnitsConsumed(10.0)// 450 -> Total = 5450.00
                .residentId("RES-402")
                .residentName("Pooja Nair")
                .advanceWalletBalance(new BigDecimal("6500.00"))
                .build();

        SocietyInvoice invoice = engine.generateInvoice(standardConfig, flat, LocalDate.of(2026, 11, 1));

        assertThat(invoice.getGrossTotalAmount()).isEqualByComparingTo(new BigDecimal("5450.00"));
        assertThat(invoice.getAdvanceWalletDeduction()).isEqualByComparingTo(new BigDecimal("5450.00"));
        assertThat(invoice.getNetPayableAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // Status must be PAID_VIA_WALLET
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID_VIA_WALLET);
        assertThat(invoice.getReceiptNumber()).isNotNull();

        // Remaining wallet balance = 6500 - 5450 = 1050.00
        assertThat(flat.getAdvanceWalletBalance()).isEqualByComparingTo(new BigDecimal("1050.00"));

        // Instant receipt created
        PaymentReceipt receipt = engine.getReceipt(invoice.getReceiptNumber());
        assertThat(receipt).isNotNull();
        assertThat(receipt.getPaymentMethod()).isEqualTo("ADVANCE_WALLET");
        assertThat(receipt.getAdvanceWalletDeducted()).isEqualByComparingTo(new BigDecimal("5450.00"));
        assertThat(receipt.getNetAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(receipt.getDigitalVerificationHash()).isNotEmpty();
    }

    @Test
    void testAdvanceWalletPartialOffsetLeavesRemainingNetPayableForUpi() {
        // Bill = ₹5,450.00, Resident has ₹2,000.00 in Advance Wallet
        FlatBillingProfile flat = FlatBillingProfile.builder()
                .flatNumber("A-505")
                .tower("Tower A")
                .carpetAreaSqFt(1200.0)
                .parkingSlotCount(1)
                .waterUnitsConsumed(10.0)
                .residentId("RES-505")
                .residentName("Karan Johar")
                .advanceWalletBalance(new BigDecimal("2000.00"))
                .build();

        SocietyInvoice invoice = engine.generateInvoice(standardConfig, flat, LocalDate.of(2026, 11, 1));

        assertThat(invoice.getGrossTotalAmount()).isEqualByComparingTo(new BigDecimal("5450.00"));
        assertThat(invoice.getAdvanceWalletDeduction()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(invoice.getNetPayableAmount()).isEqualByComparingTo(new BigDecimal("3450.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID_VIA_WALLET);

        // Wallet zeroed out
        assertThat(flat.getAdvanceWalletBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // UPI Intent URI generated for the remaining ₹3,450.00
        assertThat(invoice.getUpiIntentUri()).contains("am=3450.00");
    }

    @Test
    void testAppliesDailySimpleInterestLatePenaltyWhenOverduePastGracePeriod() {
        FlatBillingProfile flat = FlatBillingProfile.builder()
                .flatNumber("B-101")
                .tower("Tower B")
                .carpetAreaSqFt(1200.0)
                .parkingSlotCount(1)
                .waterUnitsConsumed(10.0)
                .residentId("RES-101")
                .residentName("Anil Kapoor")
                .advanceWalletBalance(BigDecimal.ZERO)
                .build();

        SocietyInvoice invoice = engine.generateInvoice(standardConfig, flat, LocalDate.of(2026, 11, 1));
        // Grace date is Nov 15, 2026
        assertThat(invoice.getGraceDate()).isEqualTo(LocalDate.of(2026, 11, 15));

        // Evaluate on Nov 30, 2026 (15 days overdue)
        LocalDate asOfDate = LocalDate.of(2026, 11, 30);
        BigDecimal penalty = engine.applyLatePenaltyIfOverdue(invoice, standardConfig, asOfDate);

        // 15 days overdue @ 18% p.a. on 5450.00:
        // Daily rate = 0.18 / 365 = 0.00049315
        // Penalty = 5450 * 0.18/365 * 15 = ~40.32
        assertThat(penalty).isGreaterThan(BigDecimal.ZERO);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(invoice.getLatePenaltyAmount()).isEqualByComparingTo(penalty);
        assertThat(invoice.getNetPayableAmount()).isEqualByComparingTo(new BigDecimal("5450.00").add(penalty));
    }

    @Test
    void testPaymentGatewayCallbackReconcilesInvoiceAndGeneratesDownloadableReceiptPdf() {
        FlatBillingProfile flat = FlatBillingProfile.builder()
                .flatNumber("D-701")
                .tower("Tower D")
                .carpetAreaSqFt(1200.0)
                .parkingSlotCount(1)
                .waterUnitsConsumed(10.0)
                .residentId("RES-701")
                .residentName("Deepika Padukone")
                .advanceWalletBalance(BigDecimal.ZERO)
                .build();

        SocietyInvoice invoice = engine.generateInvoice(standardConfig, flat, LocalDate.of(2026, 11, 1));

        // Resident pays ₹5,450.00 via Razorpay UPI
        String paymentId = "pay_RZP_9918231";
        String utr = "YESB9921827102";
        PaymentReceipt receipt = engine.processPaymentCallback(
                invoice.getInvoiceId(),
                paymentId,
                utr,
                "UPI",
                new BigDecimal("5450.00"),
                standardConfig
        );

        // Invoice status marked PAID
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getSettledUtr()).isEqualTo(utr);
        assertThat(invoice.getSettledPaymentId()).isEqualTo(paymentId);

        // Receipt attributes
        assertThat(receipt).isNotNull();
        assertThat(receipt.getReceiptNumber()).isEqualTo(invoice.getReceiptNumber());
        assertThat(receipt.getNetAmountPaid()).isEqualByComparingTo(new BigDecimal("5450.00"));
        assertThat(receipt.getPaymentMethod()).isEqualTo("UPI");
        assertThat(receipt.getUtr()).isEqualTo(utr);
        assertThat(receipt.getDigitalVerificationHash()).isNotEmpty();

        // Download rendered receipt PDF content
        String pdfContent = engine.generateReceiptPdfContent(receipt.getReceiptNumber());
        assertThat(pdfContent)
                .contains("PALM SPRINGS RWA")
                .contains("OFFICIAL MAINTENANCE RECEIPT")
                .contains("Flat No:    D-701")
                .contains("Resident: Deepika Padukone")
                .contains("NET AMOUNT PAID:             ₹5450.00")
                .contains("Bank UTR:                    YESB9921827102")
                .contains("Digital Signature (SHA-256):");
    }
}
