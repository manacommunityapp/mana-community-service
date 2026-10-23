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
public class HomeServicesTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processServiceOrder(Long residentId, Long serviceProviderId, Long communityId,
                                                           BigDecimal serviceFee, BigDecimal taxAmount,
                                                           String serviceType, String bookingNumber,
                                                           TransactionPaymentMethod method) {
        TransactionLineItem item = TransactionLineItem.builder()
                .description("Home Service: " + serviceType + " [#" + bookingNumber + "]")
                .quantity(1)
                .unitPrice(serviceFee)
                .taxAmount(taxAmount)
                .totalPrice(serviceFee)
                .build();

        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.HOME_SERVICES)
                .payerId(residentId)
                .payeeId(serviceProviderId)
                .communityId(communityId)
                .amount(serviceFee.add(taxAmount != null ? taxAmount : BigDecimal.ZERO))
                .taxAmount(taxAmount)
                .paymentMethod(method)
                .referenceType("HOME_SERVICE_BOOKING")
                .referenceId(bookingNumber)
                .narration("Home Service: " + serviceType)
                .isEscrowRequired(true) // Home service holds escrow until service completion
                .lineItems(List.of(item))
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
