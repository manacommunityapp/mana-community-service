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
public class VendorTransactionAdapter {

    private final TransactionCoreEngine transactionCoreEngine;

    public TransactionExecutionResult processVendorMilestonePayment(Long communityAdminId, Long vendorId, Long communityId,
                                                                     BigDecimal amount, BigDecimal tdsAmount,
                                                                     String contractNumber, String milestoneName) {
        TransactionLineItem item = TransactionLineItem.builder()
                .description("Vendor Contract: " + contractNumber + " - Milestone: " + milestoneName)
                .quantity(1)
                .unitPrice(amount)
                .totalPrice(amount)
                .build();

        TransactionIntent intent = TransactionIntent.builder()
                .domain(TransactionDomain.VENDOR)
                .payerId(communityAdminId)
                .payeeId(vendorId)
                .communityId(communityId)
                .amount(amount)
                .tdsAmount(tdsAmount != null ? tdsAmount : BigDecimal.ZERO)
                .paymentMethod(TransactionPaymentMethod.NET_BANKING)
                .referenceType("VMS_CONTRACT_MILESTONE")
                .referenceId(contractNumber)
                .narration("Milestone payment: " + milestoneName + " for Vendor #" + vendorId)
                .isEscrowRequired(false)
                .lineItems(List.of(item))
                .build();

        return transactionCoreEngine.executeTransaction(intent);
    }
}
