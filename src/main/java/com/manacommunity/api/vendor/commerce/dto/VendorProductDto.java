package com.manacommunity.api.vendor.commerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProductDto {
    private String id;
    private String name;
    private String productName;
    private String vendorId;
    private String productType;
    private String category;
    private String subCategory;
    private Long commerceCategoryId;
    private String commerceCategoryName;
    private String brand;
    private String description;
    private String hsnCode;
    private BigDecimal gstRate;
    private BigDecimal taxRatePercent;
    private String status;
    private String imageUrl;
    private String additionalImages;
    private List<String> images;
    private LocalDateTime createdAt;
    private Boolean communityOnly;
    private Integer weightGrams;
    private String dimensionsCm;
    private String productCode;
    private String tags;
    private List<ProductVariantDto> variants;
    private Integer totalStock;
}
