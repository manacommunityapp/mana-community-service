package com.manacommunity.api.vendor.commerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String category;
    private String subCategory;
    private String brand;
    private String description;
    private String hsnCode;
    private BigDecimal gstRate;
    private String imageUrl;

    @NotEmpty
    private List<CreateVariantRequest> variants;

    @Data
    public static class CreateVariantRequest {
        @NotBlank
        private String variantName;
        @NotBlank
        private String sku;
        private String barcode;
        @NotBlank
        private String packSize;
        private BigDecimal mrp;
        private BigDecimal vendorCost;
        private BigDecimal defaultCommunityPrice;
        private Integer initialStock;
    }
}
