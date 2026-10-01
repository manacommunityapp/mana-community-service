package com.manacommunity.api.cfbos.invoice.dto;
import com.manacommunity.api.cfbos.invoice.enums.InvoiceType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateInvoiceRequest {
    private Long propertyId;
    private Long residentId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private InvoiceType invoiceType;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String notes;
    private String terms;
    private Boolean isGstInvoice;
    private String communityGstin;
    private List<LineItemRequest> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LineItemRequest {
        private Long chargeTypeId;
        private String description;
        private String hsnSacCode;
        private BigDecimal quantity;
        private BigDecimal rate;
        private BigDecimal discountAmount;
    }
}
