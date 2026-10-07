package com.manacommunity.api.transactioncore.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.entity.CfbosWallet;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.entity.TransactionEscrow;
import com.manacommunity.api.transactioncore.entity.TransactionIntent;
import com.manacommunity.api.transactioncore.entity.TransactionSettlement;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.RefundDestination;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.transactioncore.repository.TransactionEscrowRepository;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import com.manacommunity.api.transactioncore.repository.TransactionSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManaTransactionCoreService {

    private final TransactionIntentRepository intentRepository;
    private final TransactionEscrowRepository escrowRepository;
    private final TransactionSettlementRepository settlementRepository;
    private final WalletEngine walletEngine;
    private final AccountingEngine accountingEngine;
    private final DocumentSequenceService documentSequenceService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) {
        log.info("Creating PaymentIntent domain={} orderRef={} payerId={} amount={}",
                request.getDomain(), request.getOrderReferenceId(), request.getPayerId(), request.getAmount());

        Optional<TransactionIntent> existing = intentRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing PaymentIntent intentId={} for idempotencyKey={}",
                    existing.get().getIntentId(), request.getIdempotencyKey());
            return mapToResponse(existing.get());
        }

        String intentId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String receiptNumber = "RCP-" + intentId.replace("TXN-", "");

        BigDecimal walletAmount = request.getWalletDeductionAmount() != null ? request.getWalletDeductionAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = request.getAmount();
        BigDecimal gatewayAmount = totalAmount.subtract(walletAmount);
        if (gatewayAmount.compareTo(BigDecimal.ZERO) < 0) {
            gatewayAmount = BigDecimal.ZERO;
        }

        String splitsJson = null;
        String metadataJson = null;
        try {
            if (request.getSplits() != null) {
                splitsJson = objectMapper.writeValueAsString(request.getSplits());
            }
            if (request.getMetadata() != null) {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            }
        } catch (Exception e) {
            log.warn("Failed to serialize splits or metadata: {}", e.getMessage());
        }

        TransactionIntent intent = TransactionIntent.builder()
                .intentId(intentId)
                .idempotencyKey(request.getIdempotencyKey())
                .domain(request.getDomain())
                .orderReferenceId(request.getOrderReferenceId())
                .communityId(request.getCommunityId())
                .payerId(request.getPayerId())
                .payerName(request.getPayerName())
                .payerEmail(request.getPayerEmail())
                .payerPhone(request.getPayerPhone())
                .amount(totalAmount)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(TransactionIntentStatus.INITIATED)
                .paymentRail(request.getPreferredRail() != null ? request.getPreferredRail() : PaymentRail.UPI)
                .walletDeductionAmount(walletAmount)
                .gatewayAmount(gatewayAmount)
                .escrowHeld(request.isHoldInEscrow())
                .escrowReleaseCondition(request.getEscrowReleaseCondition())
                .receiptNumber(receiptNumber)
                .splitsJson(splitsJson)
                .metadataJson(metadataJson)
                .remarks(request.getRemarks())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        if (walletAmount.compareTo(BigDecimal.ZERO) > 0 && gatewayAmount.compareTo(BigDecimal.ZERO) == 0) {
            walletEngine.debitWallet(request.getPayerId(), walletAmount,
                    WalletTransactionType.BILL_PAYMENT, request.getDomain().name(), 0L,
                    "Payment for " + request.getDomain() + " ref #" + request.getOrderReferenceId());
            intent.setStatus(request.isHoldInEscrow() ? TransactionIntentStatus.ESCROW_HELD : TransactionIntentStatus.CAPTURED);
        }

        TransactionIntent saved = intentRepository.save(intent);

        if (request.isHoldInEscrow()) {
            String escrowId = "ESC-" + intentId.replace("TXN-", "");
            TransactionEscrow escrow = TransactionEscrow.builder()
                    .escrowId(escrowId)
                    .intentId(intentId)
                    .heldAmount(totalAmount)
                    .releasedAmount(BigDecimal.ZERO)
                    .releaseCondition(request.getEscrowReleaseCondition())
                    .status("HELD")
                    .createdAt(LocalDateTime.now())
                    .build();
            escrowRepository.save(escrow);
        }

        return mapToResponse(saved);
    }

    @Transactional
    public PaymentIntentResponse confirmPayment(String intentId, String gatewayPaymentId, String gatewayOrderId) {
        TransactionIntent intent = intentRepository.findByIntentId(intentId)
                .orElseThrow(() -> new ResourceNotFoundException("TransactionIntent not found: " + intentId));

        intent.setGatewayPaymentId(gatewayPaymentId);
        if (gatewayOrderId != null) {
            intent.setGatewayOrderId(gatewayOrderId);
        }

        if (intent.isEscrowHeld()) {
            intent.setStatus(TransactionIntentStatus.ESCROW_HELD);
        } else {
            intent.setStatus(TransactionIntentStatus.CAPTURED);
            postCapturedJournalEntry(intent);
        }

        intent.setUpdatedAt(LocalDateTime.now());
        TransactionIntent updated = intentRepository.save(intent);
        return mapToResponse(updated);
    }

    @Transactional
    public SettlementResult releaseEscrow(EscrowReleaseRequest request) {
        log.info("Releasing escrow for intentId={}", request.getIntentId());

        TransactionIntent intent = intentRepository.findByIntentId(request.getIntentId())
                .orElseThrow(() -> new ResourceNotFoundException("TransactionIntent not found: " + request.getIntentId()));

        TransactionEscrow escrow = escrowRepository.findByIntentId(request.getIntentId())
                .orElseThrow(() -> new ResourceNotFoundException("Escrow record not found for intent: " + request.getIntentId()));

        if (!"HELD".equals(escrow.getStatus()) && !"PARTIALLY_RELEASED".equals(escrow.getStatus())) {
            throw new IllegalStateException("Escrow is not in HELD state. Current state: " + escrow.getStatus());
        }

        BigDecimal releaseAmount = request.getReleaseAmount() != null ? request.getReleaseAmount() : escrow.getHeldAmount();
        escrow.setReleasedAmount(escrow.getReleasedAmount().add(releaseAmount));
        escrow.setStatus(escrow.getReleasedAmount().compareTo(escrow.getHeldAmount()) >= 0 ? "RELEASED" : "PARTIALLY_RELEASED");
        escrow.setVerificationCode(request.getVerificationCode());
        escrow.setVerifiedBy(request.getVerifiedBy());
        escrow.setReleasedAt(LocalDateTime.now());
        escrow.setReleaseNotes(request.getReleaseReason());
        escrowRepository.save(escrow);

        List<PaymentSplit> splits = request.getFinalSplits();
        if (splits == null || splits.isEmpty()) {
            splits = deserializeSplits(intent.getSplitsJson());
        }

        String batchId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        List<PaymentSplit> settledSplits = new ArrayList<>();

        if (splits != null && !splits.isEmpty()) {
            for (PaymentSplit split : splits) {
                BigDecimal gross = split.getAmount() != null ? split.getAmount() : releaseAmount;
                BigDecimal commission = split.getCommissionAmount() != null ? split.getCommissionAmount() : BigDecimal.ZERO;
                BigDecimal tds = split.getTdsAmount() != null ? split.getTdsAmount() : BigDecimal.ZERO;
                BigDecimal netPayout = gross.subtract(commission).subtract(tds);

                TransactionSettlement settlement = TransactionSettlement.builder()
                        .settlementId("SET-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                        .batchId(batchId)
                        .intentId(intent.getIntentId())
                        .payeeId(split.getRecipientId() != null ? split.getRecipientId() : 0L)
                        .payeeType(split.getRecipientType() != null ? split.getRecipientType() : "VENDOR")
                        .payeeAccount(split.getRecipientAccount())
                        .grossAmount(gross)
                        .commissionAmount(commission)
                        .tdsAmount(tds)
                        .netPayoutAmount(netPayout)
                        .status("SETTLED")
                        .settledAt(LocalDateTime.now())
                        .build();

                settlementRepository.save(settlement);

                split.setNetSettlementAmount(netPayout);
                settledSplits.add(split);
            }
        }

        intent.setStatus(TransactionIntentStatus.SETTLED);
        intent.setUpdatedAt(LocalDateTime.now());
        intentRepository.save(intent);

        String journalRef = postSettlementJournalEntry(intent, releaseAmount);

        return SettlementResult.builder()
                .intentId(intent.getIntentId())
                .settlementBatchId(batchId)
                .status(TransactionIntentStatus.SETTLED)
                .totalSettled(releaseAmount)
                .splitsSettled(settledSplits)
                .journalEntryNumber(journalRef)
                .settledAt(LocalDateTime.now())
                .message("Escrow released and settlement entries created successfully")
                .build();
    }

    @Transactional
    public RefundResult processRefund(RefundRequest request) {
        log.info("Processing refund for intentId={} amount={} dest={}",
                request.getIntentId(), request.getRefundAmount(), request.getDestination());

        TransactionIntent intent = intentRepository.findByIntentId(request.getIntentId())
                .orElseThrow(() -> new ResourceNotFoundException("TransactionIntent not found: " + request.getIntentId()));

        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        if (request.getDestination() == RefundDestination.WALLET_INSTANT) {
            walletEngine.creditWallet(intent.getPayerId(), request.getRefundAmount(),
                    WalletTransactionType.REFUND, intent.getDomain().name(), intent.getId(),
                    "Refund for " + intent.getDomain() + " ref #" + intent.getOrderReferenceId() + ": " + request.getReason());
        }

        boolean isFullRefund = request.getRefundAmount().compareTo(intent.getAmount()) >= 0;
        intent.setStatus(isFullRefund ? TransactionIntentStatus.REFUNDED : TransactionIntentStatus.PARTIALLY_REFUNDED);
        intent.setUpdatedAt(LocalDateTime.now());
        intentRepository.save(intent);

        escrowRepository.findByIntentId(intent.getIntentId()).ifPresent(escrow -> {
            escrow.setRefundedAmount(escrow.getRefundedAmount().add(request.getRefundAmount()));
            if (escrow.getRefundedAmount().compareTo(escrow.getHeldAmount()) >= 0) {
                escrow.setStatus("REFUNDED");
            }
            escrowRepository.save(escrow);
        });

        String journalEntryNumber = postRefundJournalEntry(intent, request.getRefundAmount(), request.getDestination());

        return RefundResult.builder()
                .refundId(refundId)
                .intentId(intent.getIntentId())
                .amountRefunded(request.getRefundAmount())
                .destination(request.getDestination())
                .status(intent.getStatus())
                .journalEntryNumber(journalEntryNumber)
                .refundedAt(LocalDateTime.now())
                .message("Refund processed successfully")
                .build();
    }

    public BigDecimal getWalletBalance(Long residentId) {
        CfbosWallet wallet = walletEngine.getOrCreateWallet(residentId);
        return wallet != null && wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
    }

    private PaymentIntentResponse mapToResponse(TransactionIntent intent) {
        String escrowId = intent.isEscrowHeld() ? "ESC-" + intent.getIntentId().replace("TXN-", "") : null;
        List<PaymentSplit> splits = deserializeSplits(intent.getSplitsJson());

        return PaymentIntentResponse.builder()
                .intentId(intent.getIntentId())
                .idempotencyKey(intent.getIdempotencyKey())
                .domain(intent.getDomain())
                .orderReferenceId(intent.getOrderReferenceId())
                .communityId(intent.getCommunityId())
                .payerId(intent.getPayerId())
                .amount(intent.getAmount())
                .currency(intent.getCurrency())
                .status(intent.getStatus())
                .paymentRail(intent.getPaymentRail())
                .gatewayOrderId(intent.getGatewayOrderId())
                .escrowId(escrowId)
                .escrowHeld(intent.isEscrowHeld())
                .splits(splits)
                .createdAt(intent.getCreatedAt())
                .expiresAt(intent.getExpiresAt())
                .build();
    }

    private List<PaymentSplit> deserializeSplits(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PaymentSplit>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void postCapturedJournalEntry(TransactionIntent intent) {
        try {
            JournalEntryRequest jeReq = JournalEntryRequest.builder()
                    .entryDate(LocalDate.now())
                    .sourceModule(SourceModule.PAYMENT)
                    .sourceDocumentType("TRANSACTION_CORE")
                    .sourceDocumentId(intent.getId())
                    .narration("Captured payment for " + intent.getDomain() + " order #" + intent.getOrderReferenceId())
                    .lines(List.of(
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("1002")
                                    .debitAmount(intent.getAmount())
                                    .creditAmount(BigDecimal.ZERO)
                                    .narration("Debit Gateway In-Transit")
                                    .build(),
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("4001")
                                    .debitAmount(BigDecimal.ZERO)
                                    .creditAmount(intent.getAmount())
                                    .narration("Credit Revenue for " + intent.getDomain())
                                    .build()
                    ))
                    .build();
            JournalEntry entry = accountingEngine.createAndPostJournalEntry(jeReq);
            intent.setJournalEntryNumber(entry != null ? entry.getEntryNumber() : null);
        } catch (Exception e) {
            log.warn("Could not post GL journal entry for capture: {}", e.getMessage());
        }
    }

    private String postSettlementJournalEntry(TransactionIntent intent, BigDecimal amount) {
        try {
            JournalEntryRequest jeReq = JournalEntryRequest.builder()
                    .entryDate(LocalDate.now())
                    .sourceModule(SourceModule.VENDOR)
                    .sourceDocumentType("TRANSACTION_SETTLEMENT")
                    .sourceDocumentId(intent.getId())
                    .narration("Escrow settlement release for " + intent.getDomain() + " order #" + intent.getOrderReferenceId())
                    .lines(List.of(
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("2050")
                                    .debitAmount(amount)
                                    .creditAmount(BigDecimal.ZERO)
                                    .narration("Debit Escrow Holding")
                                    .build(),
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("1001")
                                    .debitAmount(BigDecimal.ZERO)
                                    .creditAmount(amount)
                                    .narration("Credit Bank Payout")
                                    .build()
                    ))
                    .build();
            JournalEntry entry = accountingEngine.createAndPostJournalEntry(jeReq);
            return entry != null ? entry.getEntryNumber() : null;
        } catch (Exception e) {
            log.warn("Could not post GL journal entry for settlement: {}", e.getMessage());
            return null;
        }
    }

    private String postRefundJournalEntry(TransactionIntent intent, BigDecimal amount, RefundDestination destination) {
        try {
            String creditAccount = destination == RefundDestination.WALLET_INSTANT ? "2010" : "1002";
            JournalEntryRequest jeReq = JournalEntryRequest.builder()
                    .entryDate(LocalDate.now())
                    .sourceModule(SourceModule.PAYMENT)
                    .sourceDocumentType("TRANSACTION_REFUND")
                    .sourceDocumentId(intent.getId())
                    .narration("Refund for " + intent.getDomain() + " order #" + intent.getOrderReferenceId())
                    .lines(List.of(
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode("4001")
                                    .debitAmount(amount)
                                    .creditAmount(BigDecimal.ZERO)
                                    .narration("Debit Revenue Reversal")
                                    .build(),
                            JournalEntryRequest.LineRequest.builder()
                                    .accountCode(creditAccount)
                                    .debitAmount(BigDecimal.ZERO)
                                    .creditAmount(amount)
                                    .narration("Credit Refund Destination (" + destination + ")")
                                    .build()
                    ))
                    .build();
            JournalEntry entry = accountingEngine.createAndPostJournalEntry(jeReq);
            return entry != null ? entry.getEntryNumber() : null;
        } catch (Exception e) {
            log.warn("Could not post GL journal entry for refund: {}", e.getMessage());
            return null;
        }
    }
}
