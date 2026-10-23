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
public class SportsTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processCourtBooking(Long residentId, Long venueId, Long communityId,
                                                           BigDecimal fee, String sportName, String courtName,
                                                           TransactionPaymentMethod method, String slotTime) {
        TransactionLineItem item = TransactionLineItem.builder()
                .description("Court Booking: " + sportName + " - " + courtName + " (" + slotTime + ")")
                .quantity(1)
                .unitPrice(fee)
                .totalPrice(fee)
                .build();

        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.SPORTS)
                .payerId(residentId)
                .payeeId(venueId)
                .communityId(communityId)
                .amount(fee)
                .paymentMethod(method)
                .referenceType("SPORTS_VENUE_BOOKING")
                .referenceId(venueId + "_" + slotTime)
                .narration("Sports Booking: " + sportName + " at " + courtName)
                .isEscrowRequired(false)
                .lineItems(List.of(item))
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
