package com.manacommunity.api.retail.unit;

import com.manacommunity.api.retail.engine.RetailInventoryEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Retail Inventory Engine Unit Tests")
class RetailInventoryEngineTest {

    private RetailInventoryEngine inventoryEngine;

    @BeforeEach
    void setUp() {
        inventoryEngine = new RetailInventoryEngine();
    }

    @Test
    @DisplayName("Should correctly calculate available stock")
    void shouldCalculateAvailableStock() {
        assertEquals(40, inventoryEngine.calculateAvailableStock(100, 60));
        assertEquals(0, inventoryEngine.calculateAvailableStock(50, 70)); // sold exceeds ordered
        assertEquals(0, inventoryEngine.calculateAvailableStock(null, null));
    }

    @Test
    @DisplayName("Should detect when reorder is required")
    void shouldDetectReorderRequired() {
        // 100 ordered - 92 sold = 8 available -> reorder level 10 -> TRUE
        assertTrue(inventoryEngine.isReorderRequired(100, 92, 10));
        // 100 ordered - 70 sold = 30 available -> reorder level 10 -> FALSE
        assertFalse(inventoryEngine.isReorderRequired(100, 70, 10));
    }

    @Test
    @DisplayName("Should calculate correct replenishment order quantity")
    void shouldCalculateReplenishmentQuantity() {
        // Stock = 5, Reorder Level = 10, Target buffer = 30 -> Target = 40 -> Needs 35
        int replenish = inventoryEngine.calculateReplenishmentQuantity(50, 45, 10, 30);
        assertEquals(35, replenish);
    }
}
