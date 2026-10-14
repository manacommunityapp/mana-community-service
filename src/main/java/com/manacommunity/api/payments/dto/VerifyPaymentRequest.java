package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code POST /api/payments/razorpay/verify}.
 *
 * <p>The frontend sends the three IDs returned by Razorpay's success handler
 * after the user completes the checkout modal. The backend verifies the
 * HMAC-SHA256 signature before marking the order as PAID.
 */
@Data
public class VerifyPaymentRequest {

    /** The Razorpay order ID stored in our DB (e.g. "order_Abc123"). */
    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    /** The Razorpay payment ID from the checkout handler (e.g. "pay_Xyz456"). */
    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    /** The HMAC-SHA256 signature from the Razorpay checkout success handler. */
    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}
