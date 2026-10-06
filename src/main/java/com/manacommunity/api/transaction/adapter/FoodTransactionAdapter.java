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
public class FoodTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processFoodDiningOrder(Long residentId, Long kitchenId, Long communityId,
                                                              BigDecimal amount, BigDecimal taxAmount,
                                                              TransactionPaymentMethod method, String orderNumber,
                                                              List<TransactionLineItem> items) {
        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.FOOD)
                .payerId(residentId)
                .payeeId(kitchenId)
                .communityId(communityId)
                .amount(amount)
                .taxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .paymentMethod(method)
                .referenceType("FOOD_ORDER")
                .referenceId(orderNumber)
                .narration("Food / Dining Order #" + orderNumber)
                .isEscrowRequired(false)
                .lineItems(items)
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
