package com.manacommunity.api.fresh.vendor.model;

import com.manacommunity.api.fresh.fulfilment.model.DietaryCategory;

public enum FreshVendorCapabilityType {
    FRUIT_SUPPLIER,
    VEGETABLE_SUPPLIER,
    FRESH_CUT_PROCESSOR,
    DAIRY_VENDOR,
    BATTER_MANUFACTURER,
    NUTRITION_VENDOR,
    DRY_FRUIT_SUPPLIER,
    CHICKEN_SUPPLIER,
    MUTTON_SUPPLIER,
    SEAFOOD_SUPPLIER,
    HEALTHY_SNACK_VENDOR,
    DELIVERY_PARTNER;

    public DietaryCategory toDietaryCategory() {
        return switch (this) {
            case CHICKEN_SUPPLIER -> DietaryCategory.POULTRY;
            case MUTTON_SUPPLIER -> DietaryCategory.MUTTON;
            case SEAFOOD_SUPPLIER -> DietaryCategory.SEAFOOD;
            default -> DietaryCategory.VEGETARIAN;
        };
    }
}
