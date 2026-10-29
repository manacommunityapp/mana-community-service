package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code POST /api/payments/cashfree/orders/{orderId}/verify}.
 *
 * <p>The frontend sends the Mana order ID returned from order creation.
 * The backend re-queries Cashfree to confirm the payment status rather
 * than trusting a client-supplied result.
 */
@Data
public class CashfreeVerifyRequest {

    /**
     * The Mana order ID (manaOrderId) from the order-creation response.
     * The SDK's onVerify callback also returns this value.
     */
    @NotBlank(message = "orderId is required")
    private String orderId;
}
