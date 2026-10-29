package com.manacommunity.api.payments.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response for Cashfree order operations.
 *
 * <p>{@link #paymentSessionId} is returned only on order creation —
 * the mobile/web SDK uses it to open the checkout sheet. It is omitted
 * from history and status queries to minimise exposure.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashfreeOrderResponse {

    private Long        id;
    private String      manaOrderId;
    private String      cfOrderId;
    private BigDecimal  amount;
    private String      currency;
    private String      status;
    private String      referenceType;
    private Long        referenceId;
    private String      description;
    private String      paymentMethod;
    private String      failureReason;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    /**
     * Returned only on order creation so the mobile/web SDK can open checkout.
     * Omitted from all other responses.
     */
    private String paymentSessionId;

    /**
     * Cashfree App ID (publishable) returned on order creation so the mobile
     * SDK can initialise the CFSession without a separate config call.
     */
    private String appId;
}
