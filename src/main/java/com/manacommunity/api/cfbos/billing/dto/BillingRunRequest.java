package com.manacommunity.api.cfbos.billing.dto;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingRunRequest {
    private Long billingScheduleId;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private String runType;
    private Boolean autoSend;
    private Long executedBy;
    private List<Long> propertyIds;
}
