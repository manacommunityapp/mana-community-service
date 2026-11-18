package com.manacommunity.api.transactioncore.event;

import com.manacommunity.api.payments.model.PaymentReferenceType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Published after a payment order is marked FAILED or EXPIRED.
 * Consumed by ModulePaymentConfirmationListener to release any reserved resources
 * (e.g. marketplace inventory).
 */
@Getter
@RequiredArgsConstructor
public class GatewayPaymentFailedEvent {

    /** Internal payment_order.id. */
    private final Long internalOrderId;
    private final PaymentReferenceType referenceType;
    private final Long referenceId;
    private final Long communityId;
    private final String failureReason;
}
