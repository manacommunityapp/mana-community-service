package com.manacommunity.api.transaction.adapter;

import com.manacommunity.api.transaction.engine.TransactionCoreEngine;
import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketplaceTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processMarketplaceOrder(Long buyerId, Long sellerId, Long communityId,
                                                               BigDecimal amount, BigDecimal taxAmount,
                                                               TransactionPaymentMethod method, String orderNumber,
                                                               List<TransactionLineItem> items, boolean useEscrow) {
        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.MARKETPLACE)
                .payerId(buyerId)
                .payeeId(sellerId)
                .communityId(communityId)
                .amount(amount)
                .taxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO)
                .paymentMethod(method)
                .referenceType("MARKETPLACE_ORDER")
                .referenceId(orderNumber)
                .narration("Marketplace Order #" + orderNumber)
                .isEscrowRequired(useEscrow)
                .lineItems(items)
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }

    public TransactionExecutionResult releaseMarketplaceEscrow(String transactionNumber, String handoverPassCode) {
        return transactionCoreEngine.releaseEscrow(TransactionEscrowReleaseRequest.builder()
                .transactionNumber(transactionNumber)
                .handoverPassCode(handoverPassCode)
                .reason("Buyer verified handover pass code")
                .build());
    }
}
