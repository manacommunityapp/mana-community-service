package com.manacommunity.api.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BusinessRuleRequest {

    @NotBlank
    private String name;

    @NotNull
    private String ruleType;

    private Long resourceId;
    private Long categoryId;
    private String description;
    private String ruleConfig;
    private String ruleKey;
    private String ruleValue;
    private String ruleOperator;
    private Integer priority;
    private Boolean isActive;

    /** ISO-8601 date (yyyy-MM-dd), parsed with LocalDate.parse in the service. */
    private String validFrom;
    private String validTo;
}
