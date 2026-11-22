package com.manacommunity.api.payments.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/payments/cashfree/orders}.
 *
 * <p>The payable amount is derived server-side from
 * {@link #referenceType} / {@link #referenceId} by
 * {@code PaymentAmountResolver}. The client's {@link #amount} is advisory
 * and is ignored for pricing.
 */
@Data
public class CashfreeCreateOrderRequest {

    /**
     * ADVISORY / IGNORED — do not wire this back into pricing.
     *
     * <p>The server prices the order from {@link #referenceType} +
     * {@link #referenceId} and charges that figure; this field is kept only so
     * existing clients keep working, and is used for two things:
     * <ol>
     *   <li>a WARN log when it disagrees with the server-derived amount
     *       (a persistent mismatch is a client bug or someone probing), and</li>
     *   <li>the one reference type that has no server-side amount yet
     *       (see {@code PaymentAmountResolver.CLIENT_PRICED_REFERENCE_TYPES}).</li>
     * </ol>
     *
     * <p>Optional: a client that omits it gets a server-priced order. The bounds
     * below are only sanity limits for the advisory value.
     */
    @DecimalMin(value = "1.00", message = "Minimum payment amount is ₹1")
    @DecimalMax(value = "99999999.99", message = "Amount exceeds maximum allowed")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    /**
     * Discriminates which module is creating this order, and — together with
     * {@link #referenceId} — determines the amount the server will charge.
     * Supported: GROUP_BUY_ORDER, COMMERCE_ORDER, SPORTS_REGISTRATION.
     * Any other value is rejected with 400.
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
