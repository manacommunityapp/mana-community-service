package com.manacommunity.api.societyerp.controller;

import com.manacommunity.api.societyerp.model.FlatBillingProfile;
import com.manacommunity.api.societyerp.model.MaintenanceBillingConfig;
import com.manacommunity.api.societyerp.model.PaymentReceipt;
import com.manacommunity.api.societyerp.model.SocietyInvoice;
import com.manacommunity.api.societyerp.service.SocietyBillingEngine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Society ERP Billing & Payments REST Controller.
 */
@RestController
@RequestMapping("/api/v1/society-erp")
@RequiredArgsConstructor
public class SocietyBillingController {

    private final SocietyBillingEngine billingEngine;

    @Getter
    @Setter
    public static class GenerateInvoiceRequest {
        private MaintenanceBillingConfig config;
        private FlatBillingProfile flat;
        private LocalDate invoiceDate;
    }

    @Getter
    @Setter
    public static class PaymentCallbackRequest {
        private String invoiceId;
        private String paymentId;
        private String utr;
        private String paymentMethod;
        private BigDecimal amountPaid;
        private MaintenanceBillingConfig config;
    }

    @PostMapping("/invoices/generate")
    public ResponseEntity<SocietyInvoice> generateInvoice(@RequestBody GenerateInvoiceRequest req) {
        LocalDate invDate = req.getInvoiceDate() != null ? req.getInvoiceDate() : LocalDate.now();
        SocietyInvoice invoice = billingEngine.generateInvoice(req.getConfig(), req.getFlat(), invDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<SocietyInvoice> getInvoice(@PathVariable String invoiceId) {
        SocietyInvoice invoice = billingEngine.getInvoice(invoiceId);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(invoice);
    }

    @PostMapping("/payments/callback")
    public ResponseEntity<PaymentReceipt> processPaymentCallback(@RequestBody PaymentCallbackRequest req) {
        PaymentReceipt receipt = billingEngine.processPaymentCallback(
                req.getInvoiceId(),
                req.getPaymentId(),
                req.getUtr(),
                req.getPaymentMethod(),
                req.getAmountPaid(),
                req.getConfig()
        );
        return ResponseEntity.ok(receipt);
    }

    @GetMapping(value = "/receipts/{receiptNumber}/pdf", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> downloadReceiptPdf(@PathVariable String receiptNumber) {
        String pdfContent = billingEngine.generateReceiptPdfContent(receiptNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + receiptNumber + ".txt\"")
                .body(pdfContent);
    }
}
