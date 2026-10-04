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
    private String category;
    private String subCategory;
    private String brand;
    private String description;
    private String hsnCode;
    private BigDecimal gstRate;
    private String status;
    private String imageUrl;
    private List<ProductVariantDto> variants;
    private Integer totalStock;
}
