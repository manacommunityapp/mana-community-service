package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonalBudgetDto {
    private String categoryId;
    private String period;
    private BigDecimal limitAmount;
    private Integer alertThreshold;
    private String month;
}
