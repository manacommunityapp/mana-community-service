package com.manacommunity.api.vendor.commerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProductDto {
    private String id;
    private String name;
    private String productType;
    private String category;
    private String subCategory;
    private Long commerceCategoryId;
    private String commerceCategoryName;
    private String brand;
    private String description;
    private String hsnCode;
    private BigDecimal gstRate;
    private String status;
    private String imageUrl;
    private String additionalImages;
    private Boolean communityOnly;
    private Integer weightGrams;
    private String dimensionsCm;
    private String productCode;
    private String tags;
    private List<ProductVariantDto> variants;
    private Integer totalStock;
}
