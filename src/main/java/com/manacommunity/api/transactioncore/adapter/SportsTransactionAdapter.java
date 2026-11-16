package com.manacommunity.api.transactioncore.adapter;

import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.RefundDestination;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Handles tournament registration and sports event payments via the transactioncore engine.
 * Court booking payments use the legacy transaction package; this adapter covers tournament
 * registration fees where escrow is held until tournament completion and organizer payout.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    /**
     * Creates a payment intent for a tournament registration fee.
     * Funds are held in escrow until the tournament completes or is cancelled.
     */
    public PaymentIntentResponse processTournamentRegistrationPayment(
            String registrationRef,
            Long tournamentId,
            Long playerId,
            String playerName,
            String playerPhone,
            String playerUnit,
            BigDecimal registrationFee,
            Long organizerUserId,
            Long communityId) {

        log.info("Creating tournament registration payment intent tournamentId={} playerId={} amount={}",
                tournamentId, playerId, registrationFee);

        PaymentSplit organizerSplit = PaymentSplit.builder()
                .recipientType("SPORTS_ORGANIZER")
                .recipientId(organizerUserId)
                .amount(registrationFee)
                .narration("Tournament #" + tournamentId + " registration fee for " + playerName)
                .build();

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("TREG-" + registrationRef)
                .domain(TransactionDomain.SPORTS)
                .orderReferenceId(registrationRef)
                .communityId(communityId)
                .payerId(playerId)
                .payerName(playerName)
                .payerPhone(playerPhone)
                .payerUnit(playerUnit)
                .title("Tournament Registration Fee")
                .amount(registrationFee)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(true)
                .escrowReleaseCondition(EscrowReleaseCondition.ON_EVENT_COMPLETION)
                .splits(List.of(organizerSplit))
                .metadata(Map.of(
                        "tournamentId", tournamentId,
                        "registrationRef", registrationRef,
                        "playerName", playerName))
                .remarks("Tournament registration fee — held until tournament completion")
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    /**
     * Creates a payment intent for a sports event entry fee (e.g., sports academy class, session fee).
     * No escrow — collected immediately.
     */
    public PaymentIntentResponse processSportsEventPayment(
            String eventRef,
            Long eventId,
            Long participantId,
            String participantName,
            String participantPhone,
            BigDecimal entryFee,
            Long communityId) {

        log.info("Creating sports event payment intent eventId={} participantId={} amount={}",
                eventId, participantId, entryFee);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("SEVT-" + eventRef)
                .domain(TransactionDomain.SPORTS)
                .orderReferenceId(eventRef)
                .communityId(communityId)
                .payerId(participantId)
                .payerName(participantName)
                .payerPhone(participantPhone)
                .title("Sports Event Entry Fee")
                .amount(entryFee)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(false)
                .metadata(Map.of("eventId", eventId, "eventRef", eventRef))
                .remarks("Sports event entry fee for event #" + eventId)
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    /**
     * Releases escrow funds to the tournament organizer after the tournament completes.
     */
    public SettlementResult releaseTournamentOrganizerPayout(String intentId, String approvedBy) {
        log.info("Releasing tournament escrow intentId={} approvedBy={}", intentId, approvedBy);

        EscrowReleaseRequest request = EscrowReleaseRequest.builder()
                .intentId(intentId)
                .verifiedBy(approvedBy)
                .releaseReason("Tournament completed — releasing registration fees to organizer")
                .build();

        return transactionCore.releaseEscrow(request);
    }

    /**
     * Refunds a tournament registration fee (e.g., tournament cancelled, player withdrawal within policy).
     */
    public RefundResult refundTournamentRegistration(String intentId, BigDecimal refundAmount, String reason) {
        log.info("Refunding tournament registration intentId={} amount={}", intentId, refundAmount);

        RefundRequest request = RefundRequest.builder()
                .intentId(intentId)
                .refundAmount(refundAmount)
                .destination(RefundDestination.WALLET_INSTANT)
                .reason(reason)
                .requestedBy("SYSTEM_SPORTS_MODULE")
                .build();

        return transactionCore.processRefund(request);
    }
}
