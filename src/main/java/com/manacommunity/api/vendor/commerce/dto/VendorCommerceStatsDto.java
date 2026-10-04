package com.manacommunity.api.vendor.commerce.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorCommerceStatsDto {
    private int totalProducts;
    private int activeProducts;
    private int outOfStockProducts;
    private int draftProducts;
    private int totalInventoryUnits;
    private int reservedUnits;
    private int committedUnits;
}
