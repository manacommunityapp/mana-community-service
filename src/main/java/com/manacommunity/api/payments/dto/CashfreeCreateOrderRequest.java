package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/payments/cashfree/orders}.
 *
 * <p>The amount is supplied by the caller, but the backend service is
 * responsible for validating it against the server-calculated price for
 * the referenced business entity. Amount from the frontend is never
 * used blindly.
 */
@Data
public class CashfreeCreateOrderRequest {

    /**
     * Amount in INR rupees. Must be at least ₹1.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum payment amount is ₹1")
    @DecimalMax(value = "99999999.99", message = "Amount exceeds maximum allowed")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    /**
     * Discriminates which module is creating this order.
     * Examples: SPORTS_REGISTRATION, EVENT_TICKET, VENUE_BOOKING.
     */
    @NotBlank(message = "referenceType is required")
    @Size(max = 60, message = "referenceType must be at most 60 characters")
    private String referenceType;

    /** PK of the business entity being paid for. */
    @NotNull(message = "referenceId is required")
    private Long referenceId;

    /** Short description shown in the Cashfree checkout sheet (optional). */
    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;

    /** Customer phone number for Cashfree customer_details (optional, 10 digits). */
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String customerPhone;
}
