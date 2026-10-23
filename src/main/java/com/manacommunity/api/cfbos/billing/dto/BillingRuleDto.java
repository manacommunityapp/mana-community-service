package com.manacommunity.api.cfbos.billing.dto;
import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BillingRuleDto {
    private Long id;
    private String name;
    private Long billingCategoryId;
    private Long chargeTypeId;
    private Long billingScheduleId;
    private CalculationMethod calculationMethod;
    private BigDecimal fixedAmount;
    private BigDecimal ratePerUnit;
    private Long formulaId;
    private Long slabConfigId;
    private Boolean isTaxable;
    private Long taxRateId;
    private Long hsnSacCodeId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private Integer priority;
    private List<BillingRuleConditionDto> conditions;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BillingRuleConditionDto {
        private String fieldName;
        private String operator;
        private String fieldValue;
        private Integer logicalGroup;
    }
}
