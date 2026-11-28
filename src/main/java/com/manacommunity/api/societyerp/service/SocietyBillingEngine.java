package com.manacommunity.api.societyerp.service;

import com.manacommunity.api.societyerp.model.FlatBillingProfile;
import com.manacommunity.api.societyerp.model.MaintenanceBillingConfig;
import com.manacommunity.api.societyerp.model.PaymentReceipt;
import com.manacommunity.api.societyerp.model.SocietyInvoice;
import com.manacommunity.api.societyerp.model.SocietyInvoice.InvoiceLineItem;
import com.manacommunity.api.societyerp.model.SocietyInvoice.InvoiceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Society ERP billing and collection lifecycle engine:
 * 1. Maintenance invoice generation with area/fixed/utility heads.
 * 2. Statutory Indian GST computation (₹7,500 threshold exemption rule).
 * 3. Daily simple interest late-payment penalty accrual.
 * 4. Automatic Resident Advance Maintenance Wallet deduction.
 * 5. Dynamic NPCI UPI Intent URI generation.
 * 6. Payment callback reconciliation and cryptographic PDF receipt generation.
 */
@Service
@Slf4j
public class SocietyBillingEngine {

    private final Map<String, SocietyInvoice> invoiceRegistry = new ConcurrentHashMap<>();
    private final Map<String, PaymentReceipt> receiptRegistry = new ConcurrentHashMap<>();

    public SocietyInvoice getInvoice(String invoiceId) {
        return invoiceRegistry.get(invoiceId);
    }

    public PaymentReceipt getReceipt(String receiptNumber) {
        return receiptRegistry.get(receiptNumber);
    }

    /**
     * Generates a monthly maintenance invoice for a flat.
     * Applies area rates, utility pass-throughs, GST threshold exemption,
     * and auto-deducts any available balance in the Resident Advance Maintenance Wallet.
     */
    public SocietyInvoice generateInvoice(
            MaintenanceBillingConfig config,
            FlatBillingProfile flat,
            LocalDate invoiceDate) {

        String billingPeriod = String.format("%d-%02d", invoiceDate.getYear(), invoiceDate.getMonthValue());
        String invoiceId = String.format("INV-%s-%s-%s", config.getSocietyId(), billingPeriod.replace("-", ""), flat.getFlatNumber());

        LocalDate dueDate = invoiceDate.withDayOfMonth(Math.min(config.getDueDayOffset(), invoiceDate.lengthOfMonth()));
        LocalDate graceDate = dueDate.plusDays(config.getGraceDays());

        List<InvoiceLineItem> lines = new ArrayList<>();

        // 1. Base Area Maintenance (Taxable)
        BigDecimal baseAreaAmt = config.getBaseRatePerSqFt()
                .multiply(BigDecimal.valueOf(flat.getCarpetAreaSqFt()))
                .setScale(2, RoundingMode.HALF_UP);
        lines.add(InvoiceLineItem.builder()
                .headName("Base Maintenance")
                .amount(baseAreaAmt)
                .taxable(true)
                .description(String.format("%.0f sq.ft @ ₹%s/sq.ft", flat.getCarpetAreaSqFt(), config.getBaseRatePerSqFt()))
                .build());

        // 2. Fixed Sinking Fund (Taxable)
        if (config.getFixedSinkingFund() != null && config.getFixedSinkingFund().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(InvoiceLineItem.builder()
                    .headName("Sinking & Reserve Fund")
                    .amount(config.getFixedSinkingFund())
                    .taxable(true)
                    .description("Capital reserve contribution")
                    .build());
        }

        // 3. Parking Slot Charges (Taxable)
        if (flat.getParkingSlotCount() > 0 && config.getParkingSlotRate() != null) {
            BigDecimal parkingAmt = config.getParkingSlotRate()
                    .multiply(BigDecimal.valueOf(flat.getParkingSlotCount()))
                    .setScale(2, RoundingMode.HALF_UP);
            lines.add(InvoiceLineItem.builder()
                    .headName("Parking Charges")
                    .amount(parkingAmt)
                    .taxable(true)
                    .description(String.format("%d slot(s) @ ₹%s/slot", flat.getParkingSlotCount(), config.getParkingSlotRate()))
                    .build());
        }

        // 4. Utility Sub-metering (Water) (Non-Taxable / Actual municipal pass-through)
        BigDecimal waterAmt = BigDecimal.ZERO;
        if (flat.getWaterUnitsConsumed() > 0 && config.getWaterRatePerUnit() != null) {
            waterAmt = config.getWaterRatePerUnit()
                    .multiply(BigDecimal.valueOf(flat.getWaterUnitsConsumed()))
                    .setScale(2, RoundingMode.HALF_UP);
            lines.add(InvoiceLineItem.builder()
                    .headName("Water Sub-Metering")
                    .amount(waterAmt)
                    .taxable(false)
                    .description(String.format("%.1f KL @ ₹%s/KL", flat.getWaterUnitsConsumed(), config.getWaterRatePerUnit()))
                    .build());
        }

        // Compute Subtotals
        BigDecimal taxableSubtotal = lines.stream()
                .filter(InvoiceLineItem::isTaxable)
                .map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nonTaxableSubtotal = lines.stream()
                .filter(l -> !l.isTaxable())
                .map(InvoiceLineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Indian GST Computation: Exempt if monthly maintenance <= ₹7,500
        boolean gstApplicable = config.isSocietyTurnoverAbove20Lakhs() &&
                taxableSubtotal.compareTo(config.getGstExemptionThreshold()) > 0;

        BigDecimal cgstRate = BigDecimal.ZERO;
        BigDecimal cgstAmt = BigDecimal.ZERO;
        BigDecimal sgstRate = BigDecimal.ZERO;
        BigDecimal sgstAmt = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        if (gstApplicable) {
            cgstRate = new BigDecimal("9.00");
            sgstRate = new BigDecimal("9.00");
            cgstAmt = taxableSubtotal.multiply(cgstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            sgstAmt = taxableSubtotal.multiply(sgstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            totalGst = cgstAmt.add(sgstAmt);
        }

        // 6. Arrears & Gross Total
        BigDecimal arrears = flat.getArrearsFromPreviousPeriod() != null ?
                flat.getArrearsFromPreviousPeriod() : BigDecimal.ZERO;

        BigDecimal grossTotal = taxableSubtotal
                .add(nonTaxableSubtotal)
                .add(totalGst)
                .add(arrears);

        // 7. Advance Maintenance Wallet Auto-Deduction
        BigDecimal walletBal = flat.getAdvanceWalletBalance() != null ?
                flat.getAdvanceWalletBalance() : BigDecimal.ZERO;

        BigDecimal walletDeduction = BigDecimal.ZERO;
        BigDecimal netPayable = grossTotal;

        if (walletBal.compareTo(BigDecimal.ZERO) > 0) {
            walletDeduction = grossTotal.min(walletBal);
            netPayable = grossTotal.subtract(walletDeduction);
            flat.setAdvanceWalletBalance(walletBal.subtract(walletDeduction));
            log.info("Auto-deducted ₹{} from Advance Wallet for Flat [{}]. Remaining wallet: ₹{}",
                    walletDeduction, flat.getFlatNumber(), flat.getAdvanceWalletBalance());
        }

        InvoiceStatus status;
        String receiptNumber = null;
        String upiIntent = null;

        if (netPayable.compareTo(BigDecimal.ZERO) == 0) {
            status = InvoiceStatus.PAID_VIA_WALLET;
            receiptNumber = "REC-" + invoiceId.replace("INV-", "") + "-WLT";
            PaymentReceipt receipt = generateReceipt(
                    receiptNumber, invoiceId, config, flat, billingPeriod,
                    grossTotal, walletDeduction, BigDecimal.ZERO,
                    "ADVANCE_WALLET", "WALLET-AUTO-" + UUID.randomUUID(), "NA"
            );
            receiptRegistry.put(receiptNumber, receipt);
        } else {
            status = walletDeduction.compareTo(BigDecimal.ZERO) > 0 ?
                    InvoiceStatus.PARTIALLY_PAID_VIA_WALLET : InvoiceStatus.PUBLISHED;

            upiIntent = generateUpiIntentUri(
                    config.getVpaAddress(),
                    config.getSocietyName(),
                    netPayable,
                    invoiceId
            );
        }

        SocietyInvoice invoice = SocietyInvoice.builder()
                .invoiceId(invoiceId)
                .societyId(config.getSocietyId())
                .societyName(config.getSocietyName())
                .flatNumber(flat.getFlatNumber())
                .tower(flat.getTower())
                .residentId(flat.getResidentId())
                .residentName(flat.getResidentName())
                .billingPeriod(billingPeriod)
                .invoiceDate(invoiceDate)
                .dueDate(dueDate)
                .graceDate(graceDate)
                .lineItems(lines)
                .taxableSubtotal(taxableSubtotal)
                .nonTaxableSubtotal(nonTaxableSubtotal)
                .gstApplicable(gstApplicable)
                .cgstRatePct(cgstRate)
                .cgstAmount(cgstAmt)
                .sgstRatePct(sgstRate)
                .sgstAmount(sgstAmt)
                .totalGstAmount(totalGst)
                .arrearsAmount(arrears)
                .latePenaltyAmount(BigDecimal.ZERO)
                .grossTotalAmount(grossTotal)
                .advanceWalletDeduction(walletDeduction)
                .netPayableAmount(netPayable)
                .status(status)
                .upiIntentUri(upiIntent)
                .receiptNumber(receiptNumber)
                .build();

        invoiceRegistry.put(invoiceId, invoice);
        return invoice;
    }

    /**
     * Accrues daily simple interest late-payment penalty if an invoice remains unpaid past its grace period.
     */
    public BigDecimal applyLatePenaltyIfOverdue(SocietyInvoice invoice, MaintenanceBillingConfig config, LocalDate asOfDate) {
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.PAID_VIA_WALLET) {
            return BigDecimal.ZERO;
        }

        if (asOfDate.isAfter(invoice.getGraceDate())) {
            long daysOverdue = ChronoUnit.DAYS.between(invoice.getGraceDate(), asOfDate);
            BigDecimal annualRate = config.getLatePenaltyAnnualInterestPct();

            // Simple daily interest: NetPayable * (AnnualRate / 100) * (daysOverdue / 365)
            BigDecimal dailyInterestRate = annualRate
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                    .divide(new BigDecimal("365"), 6, RoundingMode.HALF_UP);

            BigDecimal penalty = invoice.getNetPayableAmount()
                    .multiply(dailyInterestRate)
                    .multiply(BigDecimal.valueOf(daysOverdue))
                    .setScale(2, RoundingMode.HALF_UP);

            invoice.setLatePenaltyAmount(penalty);
            invoice.setNetPayableAmount(invoice.getNetPayableAmount().add(penalty));
            invoice.setStatus(InvoiceStatus.OVERDUE);

            // Refresh UPI intent with updated late amount
            invoice.setUpiIntentUri(generateUpiIntentUri(
                    config.getVpaAddress(),
                    config.getSocietyName(),
                    invoice.getNetPayableAmount(),
                    invoice.getInvoiceId()
            ));

            log.info("Applied late penalty of ₹{} for invoice [{}] ({} days overdue)",
                    penalty, invoice.getInvoiceId(), daysOverdue);
            return penalty;
        }
        return BigDecimal.ZERO;
    }

    /**
     * Handles payment gateway / UPI settlement callback.
     * Transitions invoice to PAID and issues a cryptographic receipt.
     */
    public PaymentReceipt processPaymentCallback(
            String invoiceId,
            String paymentId,
            String utr,
            String paymentMethod,
            BigDecimal amountPaid,
            MaintenanceBillingConfig config) {

        SocietyInvoice invoice = invoiceRegistry.get(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }

        if (amountPaid.compareTo(invoice.getNetPayableAmount()) != 0) {
            throw new IllegalArgumentException(String.format("Payment amount mismatch: Expected ₹%s but received ₹%s",
                    invoice.getNetPayableAmount(), amountPaid));
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setSettledPaymentId(paymentId);
        invoice.setSettledPaymentMethod(paymentMethod);
        invoice.setSettledUtr(utr);
        invoice.setSettledAt(Instant.now());

        String receiptNumber = "REC-" + invoiceId.replace("INV-", "") + "-PG";
        invoice.setReceiptNumber(receiptNumber);

        PaymentReceipt receipt = generateReceipt(
                receiptNumber, invoiceId, config,
                FlatBillingProfile.builder()
                        .flatNumber(invoice.getFlatNumber())
                        .residentName(invoice.getResidentName())
                        .build(),
                invoice.getBillingPeriod(),
                invoice.getGrossTotalAmount(),
                invoice.getAdvanceWalletDeduction(),
                amountPaid,
                paymentMethod,
                paymentId,
                utr
        );

        receiptRegistry.put(receiptNumber, receipt);
        log.info("Payment confirmed for invoice [{}]. Issued receipt [{}]", invoiceId, receiptNumber);
        return receipt;
    }

    private PaymentReceipt generateReceipt(
            String receiptNumber,
            String invoiceId,
            MaintenanceBillingConfig config,
            FlatBillingProfile flat,
            String billingPeriod,
            BigDecimal totalBilled,
            BigDecimal walletDeducted,
            BigDecimal netPaid,
            String paymentMethod,
            String txnRef,
            String utr) {

        Instant now = Instant.now();
        String sigPayload = String.format("%s:%s:%s:%s:%s:%s",
                receiptNumber, invoiceId, flat.getFlatNumber(), netPaid, txnRef, utr);

        String digitalHash = computeSha256(sigPayload);

        return PaymentReceipt.builder()
                .receiptNumber(receiptNumber)
                .invoiceId(invoiceId)
                .societyId(config.getSocietyId())
                .societyName(config.getSocietyName())
                .societyGstin(config.getSocietyGstin())
                .flatNumber(flat.getFlatNumber())
                .residentName(flat.getResidentName())
                .billingPeriod(billingPeriod)
                .totalBilledAmount(totalBilled)
                .advanceWalletDeducted(walletDeducted)
                .netAmountPaid(netPaid)
                .paymentMethod(paymentMethod)
                .transactionRef(txnRef)
                .utr(utr)
                .paymentTimestamp(now)
                .digitalVerificationHash(digitalHash)
                .downloadUrl("https://cdn.mygate.com/receipts/" + receiptNumber + ".pdf")
                .build();
    }

    /**
     * Generates a printable text/HTML formatted PDF representation of the receipt.
     */
    public String generateReceiptPdfContent(String receiptNumber) {
        PaymentReceipt receipt = receiptRegistry.get(receiptNumber);
        if (receipt == null) {
            throw new IllegalArgumentException("Receipt not found: " + receiptNumber);
        }

        return String.format("""
        ============================================================
                        %s
                    OFFICIAL MAINTENANCE RECEIPT
        GSTIN: %s
        ============================================================
        Receipt No: %s             Date: %s
        Invoice No: %s             Period: %s
        Flat No:    %s             Resident: %s
        ------------------------------------------------------------
        Total Billed Amount:         ₹%s
        Advance Wallet Offset:      -₹%s
        ------------------------------------------------------------
        NET AMOUNT PAID:             ₹%s
        Payment Mode:                %s
        Transaction Reference:       %s
        Bank UTR:                    %s
        ------------------------------------------------------------
        Digital Signature (SHA-256):
        %s
        ============================================================
        """,
                receipt.getSocietyName().toUpperCase(),
                receipt.getSocietyGstin() != null ? receipt.getSocietyGstin() : "NOT_REGISTERED",
                receipt.getReceiptNumber(),
                receipt.getPaymentTimestamp(),
                receipt.getInvoiceId(),
                receipt.getBillingPeriod(),
                receipt.getFlatNumber(),
                receipt.getResidentName(),
                receipt.getTotalBilledAmount(),
                receipt.getAdvanceWalletDeducted(),
                receipt.getNetAmountPaid(),
                receipt.getPaymentMethod(),
                receipt.getTransactionRef(),
                receipt.getUtr(),
                receipt.getDigitalVerificationHash()
        );
    }

    public static String generateUpiIntentUri(String vpa, String payeeName, BigDecimal amount, String invoiceId) {
        if (vpa == null || payeeName == null) return null;
        String encodedPayee = URLEncoder.encode(payeeName, StandardCharsets.UTF_8);
        return String.format(Locale.US,
                "upi://pay?pa=%s&pn=%s&am=%.2f&tr=%s&cu=INR",
                vpa, encodedPayee, amount, invoiceId);
    }

    private static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            return UUID.randomUUID().toString();
        }
    }

    public void clear() {
        invoiceRegistry.clear();
        receiptRegistry.clear();
    }
}
