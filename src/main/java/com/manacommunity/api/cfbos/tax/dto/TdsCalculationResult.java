package com.manacommunity.api.cfbos.tax.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TdsCalculationResult {
    private BigDecimal grossAmount;
    private String tdsSection;
    private BigDecimal tdsRate;
    private BigDecimal tdsAmount;
    private BigDecimal netAmount;
}
