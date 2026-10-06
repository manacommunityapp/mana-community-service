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
public class BookingTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processFacilityBooking(Long residentId, Long communityId, Long facilityId,
                                                              String facilityName, BigDecimal bookingFee,
                                                              BigDecimal depositAmount, String reservationDate) {
        BigDecimal total = bookingFee.add(depositAmount != null ? depositAmount : BigDecimal.ZERO);

        TransactionLineItem feeItem = TransactionLineItem.builder()
                .description("Facility Booking Fee: " + facilityName + " (" + reservationDate + ")")
                .quantity(1)
                .unitPrice(bookingFee)
                .totalPrice(bookingFee)
                .build();

        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.BOOKING)
                .payerId(residentId)
                .communityId(communityId)
                .amount(total)
                .paymentMethod(TransactionPaymentMethod.WALLET)
                .referenceType("FACILITY_BOOKING")
                .referenceId(facilityId + "_" + reservationDate)
                .narration("Facility reservation for " + facilityName + " on " + reservationDate)
                .isEscrowRequired(depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0)
                .lineItems(List.of(feeItem))
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
