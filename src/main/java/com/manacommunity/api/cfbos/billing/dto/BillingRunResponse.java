package com.manacommunity.api.cfbos.billing.dto;
import com.manacommunity.api.cfbos.billing.enums.BillingRunStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingRunResponse {
    private Long id;
    private String runNumber;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private Long billingScheduleId;
    private String billingScheduleName;
    private String runType;
    private BillingRunStatus status;
    private Integer totalProperties;
    private BigDecimal totalAmount;
    private BigDecimal totalTax;
    private Boolean autoSend;
    private Long executedBy;
    private LocalDateTime executedAt;
    private List<BillingRunLineDto> lines;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BillingRunLineDto {
        private Long id;
        private Long propertyId;
        private Long residentId;
        private Long billingRuleId;
        private String billingRuleName;
        private String chargeTypeCode;
        private String chargeTypeName;
        private String description;
        private BigDecimal quantity;
        private BigDecimal rate;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String calculationDetails;
    }
}
