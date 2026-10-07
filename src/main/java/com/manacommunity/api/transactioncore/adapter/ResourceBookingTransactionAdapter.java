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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceBookingTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    public PaymentIntentResponse bookAmenityWithDeposit(String bookingId, Long amenityId, Long residentId,
                                                        BigDecimal bookingFee, BigDecimal depositAmount, Long communityId) {
        BigDecimal totalAmount = bookingFee.add(depositAmount != null ? depositAmount : BigDecimal.ZERO);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("BK-" + bookingId)
                .domain(TransactionDomain.RESOURCE_BOOKING)
                .orderReferenceId(bookingId)
                .communityId(communityId)
                .payerId(residentId)
                .amount(totalAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0)
                .escrowReleaseCondition(EscrowReleaseCondition.ON_INSPECTION_PASS)
                .metadata(Map.of("amenityId", amenityId, "bookingId", bookingId, "depositAmount", depositAmount))
                .remarks("Amenity booking with refundable security deposit")
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    public RefundResult autoRefundSecurityDeposit(String intentId, BigDecimal depositAmount) {
        RefundRequest request = RefundRequest.builder()
                .intentId(intentId)
                .refundAmount(depositAmount)
                .destination(RefundDestination.WALLET_INSTANT)
                .reason("Security deposit released after amenity inspection pass")
                .requestedBy("SYSTEM_AMENITY_INSPECTION")
                .build();

        return transactionCore.processRefund(request);
    }
}
