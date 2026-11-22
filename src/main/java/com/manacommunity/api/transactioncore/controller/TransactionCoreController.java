package com.manacommunity.api.transactioncore.controller;

import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.entity.TransactionReconciliationLog;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import com.manacommunity.api.transactioncore.service.TransactionReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionCoreController {

    private final ManaTransactionCoreService transactionCoreService;
    private final TransactionReconciliationService reconciliationService;

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequest request) {
        return ResponseEntity.ok(transactionCoreService.createPaymentIntent(request));
    }

    @PostMapping("/intents/{intentId}/confirm")
    public ResponseEntity<PaymentIntentResponse> confirmPayment(
            @PathVariable String intentId,
            @RequestBody Map<String, String> confirmationData) {
        String gatewayPaymentId = confirmationData.get("gatewayPaymentId");
        String gatewayOrderId = confirmationData.get("gatewayOrderId");
        return ResponseEntity.ok(transactionCoreService.confirmPayment(intentId, gatewayPaymentId, gatewayOrderId));
    }

    @PostMapping("/escrow/release")
    public ResponseEntity<SettlementResult> releaseEscrow(
            @Valid @RequestBody EscrowReleaseRequest request) {
        return ResponseEntity.ok(transactionCoreService.releaseEscrow(request));
    }

    @PostMapping("/refunds")
    public ResponseEntity<RefundResult> processRefund(
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(transactionCoreService.processRefund(request));
    }

    @GetMapping("/wallet/{residentId}/balance")
    public ResponseEntity<Map<String, Object>> getWalletBalance(@PathVariable Long residentId) {
        BigDecimal balance = transactionCoreService.getWalletBalance(residentId);
        return ResponseEntity.ok(Map.of(
                "residentId", residentId,
                "balance", balance,
                "currency", "INR"
        ));
    }

    @PostMapping("/reconciliation/run")
    public ResponseEntity<TransactionReconciliationLog> runReconciliation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
) {
        Long communityId = principal.getCommunityId();
        return ResponseEntity.ok(reconciliationService.runDailyReconciliation(date, communityId));
    }
}
