package com.manacommunity.api.payments.service;

import com.manacommunity.api.payments.model.PaymentOrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static com.manacommunity.api.payments.model.PaymentOrderStatus.*;

/**
 * Enforces the allowed payment status transitions.
 *
 * <p>Call {@link #assertTransition} before every status update on a {@code PaymentOrder}.
 * Throws {@link IllegalStateException} for any disallowed move, ensuring the lifecycle
 * state machine is never bypassed.
 */
@Component
public class PaymentLifecycleStateMachine {

    private static final Map<PaymentOrderStatus, Set<PaymentOrderStatus>> ALLOWED =
        Map.of(
            INITIATED,          Set.of(PENDING, FAILED, EXPIRED),
            PENDING,            Set.of(SUCCESS, FAILED, EXPIRED),
            SUCCESS,            Set.of(REFUND_PENDING),
            REFUND_PENDING,     Set.of(REFUNDED, PARTIALLY_REFUNDED, SUCCESS),
            PARTIALLY_REFUNDED, Set.of(REFUND_PENDING, REFUNDED)
        );

    public void assertTransition(PaymentOrderStatus from, PaymentOrderStatus to) {
        if (from == to) return;
        Set<PaymentOrderStatus> allowed = ALLOWED.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "Invalid payment status transition: " + from + " → " + to);
        }
    }
}
