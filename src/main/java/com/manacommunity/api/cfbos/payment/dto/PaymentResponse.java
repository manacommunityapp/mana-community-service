package com.manacommunity.api.cfbos.payment.dto;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import com.manacommunity.api.cfbos.payment.enums.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentNumber;
    private LocalDate paymentDate;
    private Long residentId;
    private Long propertyId;
    private PaymentMethodType paymentMethod;
    private String paymentMode;
    private BigDecimal amount;
    private BigDecimal appliedAmount;
    private BigDecimal unappliedAmount;
    private String gatewayReference;
    private PaymentStatus status;
    private String receiptNumber;
    private Long journalEntryId;
    private String remarks;
    private List<PaymentLineDto> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentLineDto {
        private Long id;
        private Long invoiceId;
        private BigDecimal allocatedAmount;
        private LocalDate allocationDate;
    }
}
