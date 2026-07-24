package com.manacommunity.api.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private String paymentNumber;
    private VendorRef vendor;
    private Long invoiceId;
    private Long bookingId;
    private String type;
    private String status;
    private BigDecimal amount;
    private BigDecimal gstAmount;
    private BigDecimal tdsAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private String paymentMethod;
    private String transactionId;
    private LocalDate paymentDate;
    private String notes;
    private Long communityId;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class VendorRef {
        private Long id;
        private String businessName;
    }
}
