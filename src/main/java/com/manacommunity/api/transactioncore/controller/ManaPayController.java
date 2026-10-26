package com.manacommunity.api.transactioncore.controller;

import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
public class ManaPayController {

    private final ManaTransactionCoreService transactionCoreService;

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequest request) {
        log.info("[ManaPay] createPaymentIntent domain={} orderRef={} amount={}",
                request.getDomain(), request.getOrderReferenceId(), request.getAmount());
        return ResponseEntity.ok(transactionCoreService.createPaymentIntent(request));
    }

    @PostMapping("/intents/{intentId}/confirm")
    public ResponseEntity<PaymentIntentResponse> confirmPayment(
            @PathVariable String intentId,
            @RequestBody ConfirmPaymentRequest request) {
        if (request == null) {
            request = new ConfirmPaymentRequest();
        }
        request.setIntentId(intentId);
        log.info("[ManaPay] confirmPayment intentId={} rail={} utr={}",
                intentId, request.getRail(), request.getUtr());
        return ResponseEntity.ok(transactionCoreService.confirmPayment(request));
    }

    @GetMapping("/receipts/{receiptNumber}")
    public ResponseEntity<ManaDigitalReceiptDto> getDigitalReceipt(
            @PathVariable String receiptNumber) {
        log.info("[ManaPay] getDigitalReceipt identifier={}", receiptNumber);
        return ResponseEntity.ok(transactionCoreService.getDigitalReceipt(receiptNumber));
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<List<PaymentIntentResponse>> getMyTransactions(
            @RequestParam(required = false, defaultValue = "0") Long payerId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionCoreService.getMyTransactions(payerId, page, size));
    }
}
