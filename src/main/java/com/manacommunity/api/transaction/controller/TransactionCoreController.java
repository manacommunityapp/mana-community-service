package com.manacommunity.api.transaction.controller;

import com.manacommunity.api.transaction.engine.TransactionCoreEngine;
import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.model.TransactionEscrowReleaseRequest;
import com.manacommunity.api.transaction.model.TransactionExecutionResult;
import com.manacommunity.api.transaction.model.TransactionIntent;
import com.manacommunity.api.transaction.model.TransactionRefundRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Tag(name = "MANA Transaction Core", description = "Central transaction core orchestrating payments, ledger, settlement, invoice, receipt, refund, and reconciliation across all 9 domains")
public class TransactionCoreController {

    private final TransactionCoreEngine transactionCoreEngine;

    @PostMapping("/execute")
    @Operation(summary = "Execute unified transaction across any domain")
    public ResponseEntity<TransactionExecutionResult> executeTransaction(@RequestBody TransactionIntent intent) {
        return ResponseEntity.ok(transactionCoreEngine.executeTransaction(intent));
    }

    @PostMapping("/escrow/release")
    @Operation(summary = "Release escrow hold and trigger vendor payout")
    public ResponseEntity<TransactionExecutionResult> releaseEscrow(@RequestBody TransactionEscrowReleaseRequest request) {
        return ResponseEntity.ok(transactionCoreEngine.releaseEscrow(request));
    }

    @PostMapping("/refund")
    @Operation(summary = "Initiate full or partial transaction refund")
    public ResponseEntity<TransactionExecutionResult> refundTransaction(@RequestBody TransactionRefundRequest request) {
        return ResponseEntity.ok(transactionCoreEngine.refundTransaction(request));
    }

    @PostMapping("/reconcile")
    @Operation(summary = "Reconcile transaction with external bank statement UTR")
    public ResponseEntity<TransactionExecutionResult> reconcileTransaction(
            @RequestParam String transactionNumber,
            @RequestParam String utrNumber,
            @RequestParam(required = false) String gatewayRef) {
        return ResponseEntity.ok(transactionCoreEngine.reconcileTransaction(transactionNumber, utrNumber, gatewayRef));
    }

    @GetMapping("/{transactionNumber}")
    @Operation(summary = "Get transaction details by transaction number")
    public ResponseEntity<TransactionExecutionResult> getTransaction(@PathVariable String transactionNumber) {
        return ResponseEntity.ok(transactionCoreEngine.getTransaction(transactionNumber));
    }

    @GetMapping("/payer/{payerId}")
    @Operation(summary = "Get transactions for a specific resident / payer")
    public ResponseEntity<Page<TransactionExecutionResult>> getTransactionsByPayer(
            @PathVariable Long payerId, Pageable pageable) {
        return ResponseEntity.ok(transactionCoreEngine.getTransactionsByPayer(payerId, pageable));
    }

    @GetMapping("/domain/{domain}")
    @Operation(summary = "Get transactions for a specific domain")
    public ResponseEntity<Page<TransactionExecutionResult>> getTransactionsByDomain(
            @PathVariable TransactionDomain domain, Pageable pageable) {
        return ResponseEntity.ok(transactionCoreEngine.getTransactionsByDomain(domain, pageable));
    }
}
