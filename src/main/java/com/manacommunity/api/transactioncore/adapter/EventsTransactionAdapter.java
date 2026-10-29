package com.manacommunity.api.transactioncore.adapter;

import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventsTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    public PaymentIntentResponse bookEventTicket(String bookingRef, Long eventId, Long attendeeId,
                                                 BigDecimal ticketAmount, Long organizerId, Long communityId) {
        PaymentSplit organizerSplit = PaymentSplit.builder()
                .recipientType("ORGANIZER")
                .recipientId(organizerId)
                .amount(ticketAmount)
                .narration("Event #" + eventId + " Ticket Booking #" + bookingRef)
                .build();

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("EVT-" + bookingRef)
                .domain(TransactionDomain.EVENTS)
                .orderReferenceId(bookingRef)
                .communityId(communityId)
                .payerId(attendeeId)
                .amount(ticketAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(true)
                .escrowReleaseCondition(EscrowReleaseCondition.ON_EVENT_COMPLETION)
                .splits(List.of(organizerSplit))
                .metadata(Map.of("eventId", eventId, "bookingRef", bookingRef))
                .remarks("Event ticket funds held until event completion")
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    public SettlementResult releaseOrganizerPayout(String intentId, String organizerApprovedBy) {
        EscrowReleaseRequest request = EscrowReleaseRequest.builder()
                .intentId(intentId)
                .verifiedBy(organizerApprovedBy)
                .releaseReason("Event completed successfully")
                .build();

        return transactionCore.releaseEscrow(request);
    }
}
