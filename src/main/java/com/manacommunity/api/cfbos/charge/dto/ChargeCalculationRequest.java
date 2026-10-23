package com.manacommunity.api.cfbos.charge.dto;

import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeCalculationRequest {
    private CalculationMethod method;
    private CalculationMethod calculationMethod;
    private BigDecimal fixedAmount;
    private BigDecimal ratePerUnit;
    private BigDecimal quantity;
    private Long formulaId;
    private Long slabConfigId;
    private PropertyContext property;
    private PropertyContext propertyContext;

    public CalculationMethod getCalculationMethod() {
        return calculationMethod != null ? calculationMethod : method;
    }

    public PropertyContext getProperty() {
        return property != null ? property : propertyContext;
    }
}
