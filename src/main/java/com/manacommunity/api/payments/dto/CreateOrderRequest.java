package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/payments/razorpay/orders}.
 *
 * <p>The amount field is supplied by the CALLER (frontend or other service)
 * but the backend service is responsible for validating it against the
 * server-calculated price for the referenced business entity before creating
 * the Razorpay order. Amount from the frontend is NEVER used blindly.
 */
@Data
public class CreateOrderRequest {

    /**
     * Amount in INR rupees. Must be at least ₹1 (Razorpay minimum).
     * The service validates this against the server-calculated price for
     * {@link #referenceType}/{@link #referenceId}.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum payment amount is ₹1")
    @DecimalMax(value = "99999999.99", message = "Amount exceeds maximum allowed")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    /**
     * Discriminates which module is creating this order.
     * Examples: EVENT_REGISTRATION, POOJA, DONATION, FOOD_ORDER, AUCTION,
     * SPORTS_REGISTRATION, CULTURAL_REGISTRATION, OTHER.
     */
    @NotBlank(message = "referenceType is required")
    @Size(max = 60, message = "referenceType must be at most 60 characters")
    private String referenceType;

    /** PK of the business entity being paid for (CommunityEvent.id, etc.). */
    @NotNull(message = "referenceId is required")
    private Long referenceId;

    /** Short description shown in the Razorpay checkout modal (optional). */
    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;
}
