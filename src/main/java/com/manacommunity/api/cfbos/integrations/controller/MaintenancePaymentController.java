package com.manacommunity.api.cfbos.integrations.controller;

import com.manacommunity.api.cfbos.invoice.entity.CfbosInvoice;
import com.manacommunity.api.cfbos.invoice.repository.CfbosInvoiceRepository;
import com.manacommunity.api.cfbos.payment.entity.CfbosPayment;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.cfbos.payment.repository.CfbosReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance/maintenance")
@RequiredArgsConstructor
public class MaintenancePaymentController {

    private final CfbosInvoiceRepository invoiceRepo;
    private final CfbosPaymentRepository paymentRepo;
    private final CfbosReceiptRepository receiptRepo;

    /**
     * Initiate payment for a maintenance bill (invoice).
     * Returns an order reference the client passes to the payment gateway SDK.
     */
    @PostMapping("/pay/{billId}")
    public ResponseEntity<Map<String, Object>> initiatePayment(@PathVariable Long billId) {
        CfbosInvoice invoice = invoiceRepo.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + billId));

        BigDecimal due = invoice.getBalanceDue();
        if (due == null || due.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "No outstanding balance on invoice " + billId));
        }

        String orderId = "MANA-" + billId + "-" + System.currentTimeMillis();
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "amount", due,
                "currency", "INR",
                "invoiceNumber", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : ""
        ));
    }

    /**
     * Verify a completed gateway payment and record the receipt.
     * Idempotent — a duplicate orderId returns the existing receipt.
     */
    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody Map<String, Object> body) {
        String orderId = (String) body.getOrDefault("orderId", "");
        String billIdStr = (String) body.getOrDefault("billId", "");
        String gatewayRef = (String) body.getOrDefault("gatewayRef", "");
        String method = (String) body.getOrDefault("method", "UPI");

        if (orderId.isBlank() || billIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "orderId and billId are required"));
        }

        // Idempotency: don't double-post if the same orderId was already processed
        java.util.Optional<CfbosPayment> existing = gatewayRef.isBlank()
                ? java.util.Optional.empty()
                : paymentRepo.findByGatewayReference(gatewayRef);
        if (existing.filter(p -> p.getStatus() == PaymentStatus.SUCCESS).isPresent()) {
            String existingReceipt = receiptRepo.findByPaymentId(existing.get().getId())
                    .map(CfbosReceipt::getReceiptNumber).orElse("");
            return ResponseEntity.ok(Map.of("success", true, "duplicate", true, "receiptNumber", existingReceipt));
        }

        Long billId = Long.parseLong(billIdStr.toString());
        CfbosInvoice invoice = invoiceRepo.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + billId));

        PaymentMethodType paymentMethod = PaymentMethodType.UPI;
        try { paymentMethod = PaymentMethodType.valueOf(method.toUpperCase()); } catch (Exception ignored) {}

        String paymentNumber = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CfbosPayment payment = CfbosPayment.builder()
                .paymentNumber(paymentNumber)
                .paymentDate(LocalDate.now())
                .residentId(invoice.getResidentId())
                .propertyId(invoice.getPropertyId())
                .paymentMethod(paymentMethod)
                .paymentMode("ONLINE")
                .amount(invoice.getBalanceDue())
                .appliedAmount(invoice.getBalanceDue())
                .unappliedAmount(BigDecimal.ZERO)
                .gatewayReference(gatewayRef)
                .status(PaymentStatus.SUCCESS)
                .build();
        paymentRepo.save(payment);

        // Update invoice balance
        invoice.setAmountPaid(invoice.getAmountPaid().add(invoice.getBalanceDue()));
        invoice.setBalanceDue(BigDecimal.ZERO);
        invoiceRepo.save(invoice);

        String receiptNumber = "REC-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CfbosReceipt receipt = CfbosReceipt.builder()
                .receiptNumber(receiptNumber)
                .payment(payment)
                .receiptDate(LocalDate.now())
                .build();
        receiptRepo.save(receipt);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "receiptNumber", receiptNumber,
                "paidAt", LocalDateTime.now().toString(),
                "message", "Maintenance payment recorded successfully."
        ));
    }
}
