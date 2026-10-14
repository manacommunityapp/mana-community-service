package com.manacommunity.api.transactioncore.dto;

import com.manacommunity.api.transactioncore.enums.PaymentRail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {
    private String intentId;
    private PaymentRail rail;
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String gatewaySignature;
    private String utr;
    private String bankRefNumber;
}
