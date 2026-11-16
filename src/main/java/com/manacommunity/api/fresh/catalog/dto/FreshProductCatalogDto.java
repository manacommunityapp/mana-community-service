package com.manacommunity.api.fresh.catalog.dto;

import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class FreshProductCatalogDto {
    private Long vendorProductId;
    private String productName;
    private FreshVendorCapabilityType capabilityType;
    // Catalog profile
    private Integer shelfLifeHours;
    private String storageInstruction;
    private String originState;
    private Boolean isOrganic;
    private Boolean isPesticideFree;
    private Boolean isAntibioticFree;
    private Boolean isHormoneFree;
    private BigDecimal minOrderQty;
    private BigDecimal maxOrderQty;
    private String orderUnit;
    // Current active pricing
    private FreshProductPricingDto activePricing;
    // Dynamic attributes
    private List<FreshProductAttributeDto> attributes;
    // Nutrition
    private FreshProductNutritionDto nutrition;
    // Allergens
    private List<FreshProductAllergenDto> allergens;
}