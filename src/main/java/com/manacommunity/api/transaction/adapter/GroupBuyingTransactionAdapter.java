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
public class GroupBuyingTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processGroupBuyParticipation(Long participantId, Long supplierId, Long communityId,
                                                                   BigDecimal committedAmount, Long dealId, String dealTitle,
                                                                   int quantity, TransactionPaymentMethod method) {
        TransactionLineItem item = TransactionLineItem.builder()
                .description("Group Buy Deal: " + dealTitle)
                .quantity(quantity)
                .unitPrice(committedAmount.divide(BigDecimal.valueOf(Math.max(1, quantity)), 2, RoundingMode.HALF_UP))
                .totalPrice(committedAmount)
                .build();

        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.GROUP_BUYING)
                .payerId(participantId)
                .payeeId(supplierId)
                .communityId(communityId)
                .amount(committedAmount)
                .paymentMethod(method)
                .referenceType("GROUP_BUY_DEAL")
                .referenceId(String.valueOf(dealId))
                .narration("Group Buy Participation: " + dealTitle)
                .isEscrowRequired(true) // Group buying holds escrow until target volume reached & shipped
                .lineItems(List.of(item))
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
