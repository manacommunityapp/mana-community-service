package com.manacommunity.api.vendor.commerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateProductRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    /** Product type: PHYSICAL_PRODUCT, DIGITAL_PRODUCT, SERVICE, FOOD, BOOKING, SUBSCRIPTION */
    private String productType;

    @NotBlank
    @Size(max = 80)
    private String category;

    @Size(max = 80)
    private String subCategory;

    /** Optional: link to a structured commerce category. */
    private Long commerceCategoryId;

    @Size(max = 120)
    private String brand;

    private String description;

    @Size(max = 30)
    private String hsnCode;

    @DecimalMin("0.0")
    private BigDecimal gstRate;

    private String imageUrl;

    /** Comma-separated additional image URLs. */
    private String additionalImages;

    /** If true, only community members can see and purchase this product. */
    private Boolean communityOnly;

    private Integer weightGrams;

    @Size(max = 60)
    private String dimensionsCm;

    @Size(max = 60)
    private String productCode;

    private String tags;

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

        @DecimalMin("0.0")
        private BigDecimal mrp;
        @DecimalMin("0.0")
        private BigDecimal vendorCost;
        @DecimalMin("0.0")
        private BigDecimal defaultCommunityPrice;

        // Community pricing tiers
        @DecimalMin("0.0")
        private BigDecimal memberPrice;
        private Integer bulkMinQty;
        @DecimalMin("0.0")
        private BigDecimal bulkPrice;
        @DecimalMin("0.0")
        private BigDecimal promotionalPrice;
        private LocalDate promotionalEndDate;
        private String festivalTag;
        @DecimalMin("0.0")
        private BigDecimal festivalPrice;

        private Integer initialStock;
    }
}
