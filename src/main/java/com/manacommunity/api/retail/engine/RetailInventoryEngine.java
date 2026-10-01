package com.manacommunity.api.retail.engine;

import org.springframework.stereotype.Component;

@Component
public class RetailInventoryEngine {

    /**
     * Computes available stock from units ordered (purchased) and units sold.
     */
    public int calculateAvailableStock(Integer unitsOrdered, Integer unitsSold) {
        int ordered = unitsOrdered != null ? unitsOrdered : 0;
        int sold = unitsSold != null ? unitsSold : 0;
        return Math.max(0, ordered - sold);
    }

    /**
     * Determines whether a product has breached its reorder threshold.
     */
    public boolean isReorderRequired(Integer unitsOrdered, Integer unitsSold, Integer reorderLevel) {
        int stock = calculateAvailableStock(unitsOrdered, unitsSold);
        int threshold = reorderLevel != null ? reorderLevel : 10;
        return stock <= threshold;
    }

    /**
     * Calculates recommended replenishment order quantity to achieve safe target stock level.
     */
    public int calculateReplenishmentQuantity(Integer unitsOrdered, Integer unitsSold, Integer reorderLevel, int targetBufferUnits) {
        int currentStock = calculateAvailableStock(unitsOrdered, unitsSold);
        int threshold = reorderLevel != null ? reorderLevel : 10;
        int target = threshold + Math.max(targetBufferUnits, 20);
        return Math.max(0, target - currentStock);
    }
}
