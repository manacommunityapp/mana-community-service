package com.manacommunity.api.dto;

import com.manacommunity.api.model.PaymentMode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentRequest {
    private PaymentMode paymentMode;
    private String paymentReference;
    private LocalDateTime paidAt;
}
