package com.manacommunity.api.cfbos.charge.dto;

import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeCalculationRequest {
    private CalculationMethod method;
    private BigDecimal fixedAmount;
    private BigDecimal ratePerUnit;
    private PropertyContext propertyContext;
}
