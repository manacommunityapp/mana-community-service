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
public class MarketplaceTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    public PaymentIntentResponse initiateMarketplaceOrder(String orderId, Long buyerId, Long sellerId,
                                                          BigDecimal totalAmount, BigDecimal commissionRate, Long communityId) {
        BigDecimal commission = totalAmount.multiply(commissionRate != null ? commissionRate : BigDecimal.valueOf(0.05));
        BigDecimal netSellerPayout = totalAmount.subtract(commission);

        PaymentSplit sellerSplit = PaymentSplit.builder()
                .recipientType("MERCHANT")
                .recipientId(sellerId)
                .amount(totalAmount)
                .commissionAmount(commission)
                .netSettlementAmount(netSellerPayout)
                .narration("Marketplace Order #" + orderId)
                .build();

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("MKT-" + orderId)
                .domain(TransactionDomain.MARKETPLACE)
                .orderReferenceId(orderId)
                .communityId(communityId)
                .payerId(buyerId)
                .amount(totalAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(true)
                .escrowReleaseCondition(EscrowReleaseCondition.ON_DELIVERY_VERIFICATION)
                .splits(List.of(sellerSplit))
                .metadata(Map.of("orderId", orderId, "sellerId", sellerId))
                .remarks("Marketplace order payment held in escrow")
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    public SettlementResult verifyDeliveryAndReleaseSeller(String intentId, String deliveryOtp) {
        EscrowReleaseRequest request = EscrowReleaseRequest.builder()
                .intentId(intentId)
                .verificationCode(deliveryOtp)
                .releaseReason("Delivery verified via buyer OTP")
                .build();

        return transactionCore.releaseEscrow(request);
    }
}
