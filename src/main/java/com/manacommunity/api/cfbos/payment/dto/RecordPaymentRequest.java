package com.manacommunity.api.cfbos.payment.dto;
import com.manacommunity.api.cfbos.payment.enums.PaymentMethodType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RecordPaymentRequest {
    private Long residentId;
    private Long propertyId;
    private BigDecimal amount;
    private PaymentMethodType paymentMethod;
    private String paymentMode;
    private LocalDate paymentDate;
    private String gatewayReference;
    private String remarks;
    private List<Long> specificInvoiceIds;
}
