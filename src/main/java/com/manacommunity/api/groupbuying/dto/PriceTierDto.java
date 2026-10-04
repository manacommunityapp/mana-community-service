package com.manacommunity.api.groupbuying.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceTierDto {
    private String id;
    private Integer minQty;
    private Integer maxQty;
    private BigDecimal price;
    private String label;
    private Boolean isCurrentTier;
    private Boolean isNextTier;
    private Integer unitsToUnlock;
    private BigDecimal savingsVsMrp;
}
