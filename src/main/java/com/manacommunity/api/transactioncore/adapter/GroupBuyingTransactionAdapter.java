package com.manacommunity.api.transactioncore.adapter;

import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.RefundDestination;
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
public class GroupBuyingTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    public PaymentIntentResponse initiateGroupOrderPayment(String orderNumber, Long dealId, Long residentId,
                                                            BigDecimal initialAmount, Long vendorId, Long communityId) {
        PaymentSplit vendorSplit = PaymentSplit.builder()
                .recipientType("VENDOR")
                .recipientId(vendorId)
                .amount(initialAmount)
                .narration("Group Buy Deal #" + dealId + " Order #" + orderNumber)
                .build();

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("GB-" + orderNumber)
                .domain(TransactionDomain.GROUP_BUYING)
                .orderReferenceId(orderNumber)
                .communityId(communityId)
                .payerId(residentId)
                .amount(initialAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(true)
                .escrowReleaseCondition(EscrowReleaseCondition.ON_DELIVERY_VERIFICATION)
                .splits(List.of(vendorSplit))
                .metadata(Map.of("dealId", dealId, "orderNumber", orderNumber))
                .remarks("Group buy order payment held in escrow until pickup")
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    public SettlementResult completePickupAndReleaseVendor(String intentId, String qrCode, String verifiedByGuard) {
        EscrowReleaseRequest releaseReq = EscrowReleaseRequest.builder()
                .intentId(intentId)
                .verificationCode(qrCode)
                .verifiedBy(verifiedByGuard)
                .releaseReason("Pickup verified by QR pass scan")
                .build();

        return transactionCore.releaseEscrow(releaseReq);
    }

    public RefundResult refundTierSavingsDelta(String intentId, BigDecimal savingsDelta, String dealTitle) {
        RefundRequest refundReq = RefundRequest.builder()
                .intentId(intentId)
                .refundAmount(savingsDelta)
                .destination(RefundDestination.WALLET_INSTANT)
                .reason("Group buying tier reached higher discount: " + dealTitle)
                .requestedBy("SYSTEM_TIER_ENGINE")
                .build();

        return transactionCore.processRefund(refundReq);
    }
}
