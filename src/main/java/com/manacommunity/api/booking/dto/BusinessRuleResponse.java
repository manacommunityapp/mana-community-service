package com.manacommunity.api.booking.dto;

import com.manacommunity.api.booking.entity.enums.RuleType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BusinessRuleResponse {

    private Long id;
    private Long resourceId;
    private Long categoryId;
    private RuleType ruleType;
    private String ruleKey;
    private String ruleValue;
    private String ruleOperator;
    private String description;
    private Boolean isActive;
    private Integer priority;
    private String validFrom;
    private String validTo;
}
