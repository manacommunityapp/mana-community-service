package com.manacommunity.api.vendor.commerce.dto;

import lombok.*;
import java.math.BigDecimal;

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
    private BigDecimal mrp;
    private BigDecimal vendorCost;
    private BigDecimal defaultCommunityPrice;
    private Boolean isActive;
    private Integer availableStock;
    private Integer reservedStock;
    private Integer committedStock;
}
