package com.manacommunity.api.cfbos.charge.dto;

import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChargeCalculationResult {
    private CalculationMethod method;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal rate;
    private String calculationDetails;
}
