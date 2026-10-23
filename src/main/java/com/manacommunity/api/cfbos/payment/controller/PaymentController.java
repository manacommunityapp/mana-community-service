package com.manacommunity.api.cfbos.payment.controller;

import com.manacommunity.api.cfbos.payment.dto.PaymentResponse;
import com.manacommunity.api.cfbos.payment.dto.RecordPaymentRequest;
import com.manacommunity.api.cfbos.payment.entity.CfbosReceipt;
import com.manacommunity.api.cfbos.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cfbos/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> recordPayment(@RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(paymentService.recordPayment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<List<PaymentResponse>> getByResident(@PathVariable Long residentId) {
        return ResponseEntity.ok(paymentService.getPaymentsForResident(residentId));
    }

    @GetMapping("/resident/{residentId}/receipts")
    public ResponseEntity<List<CfbosReceipt>> getReceipts(@PathVariable Long residentId) {
        return ResponseEntity.ok(paymentService.getReceiptsForResident(residentId));
    }
}
