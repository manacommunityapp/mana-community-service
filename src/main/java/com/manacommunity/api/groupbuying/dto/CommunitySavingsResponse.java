package com.manacommunity.api.groupbuying.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunitySavingsResponse {
    private BigDecimal totalSavedThisMonth;
    private Integer totalOrders;
    private Integer activeDeals;
    private BigDecimal avgSavingPerOrder;
    private Integer totalKgsBought;
    private String topCategory;
    private BigDecimal totalSavedAllTime;
}
