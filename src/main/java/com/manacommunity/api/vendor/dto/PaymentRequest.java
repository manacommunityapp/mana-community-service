package com.manacommunity.api.vendor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRequest {
    @NotNull
    private Long vendorId;
    private Long invoiceId;
    private Long bookingId;
    private String type;
    @NotNull
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
    private LocalDate paymentDate;
    private String notes;
}
