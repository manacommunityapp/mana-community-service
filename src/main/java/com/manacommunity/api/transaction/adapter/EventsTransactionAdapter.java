package com.manacommunity.api.transaction.adapter;

import com.manacommunity.api.transaction.engine.TransactionCoreEngine;
import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.model.TransactionExecutionResult;
import com.manacommunity.api.transaction.model.TransactionIntent;
import com.manacommunity.api.transaction.model.TransactionLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventsTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processEventBooking(Long residentId, Long organizerId, Long communityId,
                                                           BigDecimal ticketAmount, Long eventId, String eventTitle,
                                                           TransactionPaymentMethod method, int ticketCount) {
        TransactionLineItem item = TransactionLineItem.builder()
                .description("Event Ticket: " + eventTitle)
                .quantity(ticketCount)
                .unitPrice(ticketAmount.divide(BigDecimal.valueOf(Math.max(1, ticketCount)), 2, RoundingMode.HALF_UP))
                .totalPrice(ticketAmount)
                .build();

        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.EVENTS)
                .payerId(residentId)
                .payeeId(organizerId)
                .communityId(communityId)
                .amount(ticketAmount)
                .paymentMethod(method)
                .referenceType("EVENT_BOOKING")
                .referenceId(String.valueOf(eventId))
                .narration("Booking for Event: " + eventTitle)
                .isEscrowRequired(false)
                .lineItems(List.of(item))
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
