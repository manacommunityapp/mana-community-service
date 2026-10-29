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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processTripBooking(Long passengerId, Long driverId, Long communityId,
                                                          BigDecimal fareAmount, String tripId, String pickup, String drop) {
        TransactionLineItem item = TransactionLineItem.builder()
                .description("Commute Trip: " + pickup + " to " + drop)
                .quantity(1)
                .unitPrice(fareAmount)
                .totalPrice(fareAmount)
                .build();

        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.TRAVEL)
                .payerId(passengerId)
                .payeeId(driverId)
                .communityId(communityId)
                .amount(fareAmount)
                .paymentMethod(TransactionPaymentMethod.WALLET)
                .referenceType("COMMUTE_TRIP")
                .referenceId(tripId)
                .narration("Commute Ride Fare: " + pickup + " -> " + drop)
                .isEscrowRequired(false)
                .lineItems(List.of(item))
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
