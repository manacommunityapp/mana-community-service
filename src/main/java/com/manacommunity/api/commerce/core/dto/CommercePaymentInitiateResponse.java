package com.manacommunity.api.commerce.core.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CommercePaymentInitiateResponse {
    String orderNumber;
    String razorpayOrderId;
    BigDecimal amount;
    String currency;
    String keyId;
    String description;
}
