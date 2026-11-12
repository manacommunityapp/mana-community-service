package com.manacommunity.api.fresh.fulfilment.model;

/**
 * FSSAI Schedule 4 Food Safety Segregation Category.
 * Strict segregation required between vegetarian items (batters, dairy, produce)
 * and raw meats (poultry, mutton, seafood) to prevent cross-contamination.
 */
public enum DietaryCategory {
    VEGETARIAN,
    POULTRY,
    MUTTON,
    SEAFOOD
}
