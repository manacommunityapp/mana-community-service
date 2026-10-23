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
public class HomeServicesTransactionAdapter {

    private final ManaTransactionCoreService transactionCore;

    public PaymentIntentResponse initiateServicePayment(String workOrderNumber, Long serviceRequestId, Long residentId,
                                                        BigDecimal serviceAmount, Long providerId, Long communityId) {
        PaymentSplit providerSplit = PaymentSplit.builder()
                .recipientType("TECHNICIAN")
                .recipientId(providerId)
                .amount(serviceAmount)
                .narration("WorkOrder #" + workOrderNumber + " Service Fee")
                .build();

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .idempotencyKey("SRV-" + workOrderNumber)
                .domain(TransactionDomain.HOME_SERVICES)
                .orderReferenceId(workOrderNumber)
                .communityId(communityId)
                .payerId(residentId)
                .amount(serviceAmount)
                .preferredRail(PaymentRail.UPI)
                .holdInEscrow(true)
                .escrowReleaseCondition(EscrowReleaseCondition.ON_SERVICE_OTP)
                .splits(List.of(providerSplit))
                .metadata(Map.of("workOrderNumber", workOrderNumber, "serviceRequestId", serviceRequestId))
                .remarks("Service fee held in escrow until work completion")
                .build();

        return transactionCore.createPaymentIntent(request);
    }

    public SettlementResult verifyServiceOtpAndRelease(String intentId, String serviceOtp) {
        EscrowReleaseRequest request = EscrowReleaseRequest.builder()
                .intentId(intentId)
                .verificationCode(serviceOtp)
                .releaseReason("Service completed and resident OTP verified")
                .build();

        return transactionCore.releaseEscrow(request);
    }
}
