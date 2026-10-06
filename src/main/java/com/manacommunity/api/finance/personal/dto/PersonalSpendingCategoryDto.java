package com.manacommunity.api.finance.personal.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalSpendingCategoryDto {
    private String key;
    private String label;
    private String icon;
    private String color;
    private BigDecimal amount;
    private int percentage;
    private int transactionCount;
}
