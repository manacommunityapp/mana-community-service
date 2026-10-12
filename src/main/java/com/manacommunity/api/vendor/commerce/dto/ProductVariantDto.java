package com.manacommunity.api.vendor.commerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDto {
    private String id;
    private String variantName;
    private String sku;
    private String barcode;
    private String packSize;

    // Core pricing
    private BigDecimal mrp;
    private BigDecimal vendorCost;
    private BigDecimal defaultCommunityPrice;

    // Community pricing tiers
    private BigDecimal memberPrice;
    private Integer bulkMinQty;
    private BigDecimal bulkPrice;
    private BigDecimal promotionalPrice;
    private LocalDate promotionalEndDate;
    private String festivalTag;
    private BigDecimal festivalPrice;

    private Boolean isActive;
    private Integer availableStock;
    private Integer reservedStock;
    private Integer committedStock;
}
