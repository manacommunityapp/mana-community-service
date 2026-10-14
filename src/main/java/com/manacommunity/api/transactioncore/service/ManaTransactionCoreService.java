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
import com.manacommunity.api.notification.event.PaymentCapturedEvent;
import com.manacommunity.api.transactioncore.event.TransactionCapturedEvent;
import com.manacommunity.api.transactioncore.repository.TransactionEscrowRepository;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import com.manacommunity.api.transactioncore.repository.TransactionSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        Long payerId = request.getPayerId() != null ? request.getPayerId() : 0L;
        String title = request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : "Mana " + request.getDomain() + " Payment";
        String upiVpa = "mana.society@icici";
        String encodedTitle = "";
        try {
            encodedTitle = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        String upiIntentUri = String.format(java.util.Locale.US, "upi://pay?pa=%s&pn=Mana%%20Community&am=%.2f&cu=INR&tr=%s&tn=%s",
                upiVpa, totalAmount, intentId, encodedTitle);

        TransactionIntent intent = TransactionIntent.builder()
                .intentId(intentId)
                .idempotencyKey(request.getIdempotencyKey())
                .domain(request.getDomain())
                .orderReferenceId(request.getOrderReferenceId())
                .communityId(request.getCommunityId())
                .payerId(payerId)
                .payerName(request.getPayerName())
                .payerEmail(request.getPayerEmail())
                .payerPhone(request.getPayerPhone())
                .payerUnit(request.getPayerUnit())
                .title(title)
                .amount(totalAmount)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .status(TransactionIntentStatus.INITIATED)
                .paymentRail(request.getPreferredRail() != null ? request.getPreferredRail() : PaymentRail.UPI)
                .walletDeductionAmount(walletAmount)
                .gatewayAmount(gatewayAmount)
                .upiVpa(upiVpa)
                .upiIntentUri(upiIntentUri)
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
            walletEngine.debitWallet(payerId, walletAmount,
                    WalletTransactionType.BILL_PAYMENT, request.getDomain().name(), 0L,
                    "Payment for " + request.getDomain() + " ref #" + request.getOrderReferenceId());
            boolean escrow = request.isHoldInEscrow();
            intent.setStatus(escrow ? TransactionIntentStatus.ESCROW_HELD : TransactionIntentStatus.CAPTURED);
            intent.setPaidAt(LocalDateTime.now());
            if (!escrow) {
                postCapturedJournalEntry(intent);
            }
        }

        TransactionIntent saved = intentRepository.save(intent);

        if (saved.getStatus() == TransactionIntentStatus.CAPTURED || saved.getStatus() == TransactionIntentStatus.ESCROW_HELD) {
            publishCaptureEvents(saved);
        }

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
        return confirmPayment(ConfirmPaymentRequest.builder()
                .intentId(intentId)
                .gatewayPaymentId(gatewayPaymentId)
                .gatewayOrderId(gatewayOrderId)
                .build());
    }

    @Transactional
    public PaymentIntentResponse confirmPayment(ConfirmPaymentRequest req) {
        TransactionIntent intent = intentRepository.findByIntentId(req.getIntentId())
                .orElseThrow(() -> new ResourceNotFoundException("TransactionIntent not found: " + req.getIntentId()));

        if (req.getGatewayPaymentId() != null) {
            intent.setGatewayPaymentId(req.getGatewayPaymentId());
        }
        if (req.getGatewayOrderId() != null) {
            intent.setGatewayOrderId(req.getGatewayOrderId());
        }
        if (req.getUtr() != null) {
            intent.setUtr(req.getUtr());
        }
        if (req.getRail() != null) {
            intent.setPaymentRail(req.getRail());
        }

        intent.setPaidAt(LocalDateTime.now());

        if (intent.isEscrowHeld()) {
            intent.setStatus(TransactionIntentStatus.ESCROW_HELD);
        } else {
            intent.setStatus(TransactionIntentStatus.CAPTURED);
            postCapturedJournalEntry(intent);
        }

        intent.setUpdatedAt(LocalDateTime.now());
        TransactionIntent updated = intentRepository.save(intent);
        publishCaptureEvents(updated);
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
                .gatewayKeyId("rzp_test_placeholder")
                .upiVpa(intent.getUpiVpa())
                .upiIntentUri(intent.getUpiIntentUri())
                .upiQrString(intent.getUpiIntentUri())
                .receiptNumber(intent.getReceiptNumber())
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

    public ManaDigitalReceiptDto getDigitalReceipt(String receiptOrIntentNumber) {
        TransactionIntent intent = intentRepository.findByReceiptNumber(receiptOrIntentNumber)
                .or(() -> intentRepository.findByIntentId(receiptOrIntentNumber))
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for identifier: " + receiptOrIntentNumber));

        return ManaDigitalReceiptDto.builder()
                .receiptNumber(intent.getReceiptNumber())
                .receiptDate(intent.getPaidAt() != null ? intent.getPaidAt() : intent.getCreatedAt())
                .transactionNumber(intent.getIntentId())
                .orderReferenceId(intent.getOrderReferenceId())
                .domain(intent.getDomain())
                .status(intent.getStatus())
                .payerName(intent.getPayerName() != null ? intent.getPayerName() : "Community Member")
                .payerUnit(intent.getPayerUnit() != null ? intent.getPayerUnit() : "Resident Unit")
                .payerPhone(intent.getPayerPhone())
                .paymentRail(intent.getPaymentRail())
                .amount(intent.getAmount())
                .taxAmount(BigDecimal.ZERO)
                .convenienceFee(BigDecimal.ZERO)
                .totalAmount(intent.getAmount())
                .utr(intent.getUtr())
                .gatewayRef(intent.getGatewayPaymentId())
                .societyName("Mana Residency Owners Association")
                .societyGstin("29AABCM1234F1Z5")
                .societyAddress("Sarjapur Main Road, Bengaluru, Karnataka 560035")
                .notes(intent.getRemarks() != null ? intent.getRemarks() : intent.getTitle())
                .verifiedStamp(true)
                .build();
    }

    public List<PaymentIntentResponse> getMyTransactions(Long payerId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return intentRepository.findByPayerId(payerId, pageable)
                .map(this::mapToResponse)
                .getContent();
    }

    private void publishCaptureEvents(TransactionIntent intent) {
        try {
            eventPublisher.publishEvent(TransactionCapturedEvent.builder()
                    .intentId(intent.getIntentId())
                    .orderReferenceId(intent.getOrderReferenceId())
                    .domain(intent.getDomain())
                    .payerId(intent.getPayerId())
                    .payerName(intent.getPayerName())
                    .payerPhone(intent.getPayerPhone())
                    .payerUnit(intent.getPayerUnit())
                    .amount(intent.getAmount())
                    .paymentRail(intent.getPaymentRail())
                    .utr(intent.getUtr())
                    .gatewayPaymentId(intent.getGatewayPaymentId())
                    .gatewayOrderId(intent.getGatewayOrderId())
                    .receiptNumber(intent.getReceiptNumber())
                    .capturedAt(intent.getPaidAt() != null ? intent.getPaidAt() : LocalDateTime.now())
                    .intent(intent)
                    .build());

            if (intent.getPayerPhone() != null && !intent.getPayerPhone().isBlank()) {
                eventPublisher.publishEvent(new PaymentCapturedEvent(
                        intent.getPayerId(),
                        intent.getPayerPhone(),
                        intent.getPayerName() != null ? intent.getPayerName() : "Community Member",
                        intent.getAmount(),
                        intent.getOrderReferenceId(),
                        intent.getGatewayPaymentId() != null ? intent.getGatewayPaymentId() : intent.getIntentId()
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to publish capture events for intentId={}: {}", intent.getIntentId(), e.getMessage());
        }
    }
}
