package com.manacommunity.api.serviceplatform.unit;

import com.manacommunity.api.serviceplatform.material.engine.MaterialEngine;
import com.manacommunity.api.serviceplatform.material.entity.ServiceMaterial;
import com.manacommunity.api.serviceplatform.material.entity.WorkOrderMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Material Engine Unit Tests")
class MaterialEngineTest {

    private MaterialEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MaterialEngine();
    }

    @Test
    @DisplayName("Should calculate material price total correctly")
    void testCalculateMaterialTotal() {
        ServiceMaterial mat = ServiceMaterial.builder()
                .itemName("Copper Pipe")
                .unitPrice(new BigDecimal("250.00"))
                .build();

        BigDecimal total = engine.calculateMaterialTotal(mat, 4);
        assertEquals(new BigDecimal("1000.00"), total);
    }

    @Test
    @DisplayName("Should deduct stock and throw if insufficient")
    void testStockDeduction() {
        ServiceMaterial mat = ServiceMaterial.builder()
                .itemName("Valve")
                .stockQuantity(10)
                .build();

        engine.deductStock(mat, 4);
        assertEquals(6, mat.getStockQuantity());

        assertThrows(IllegalStateException.class, () -> engine.deductStock(mat, 10));
    }

    @Test
    @DisplayName("Should sum total work order materials cost")
    void testCalculateTotalMaterialsCost() {
        List<WorkOrderMaterial> list = List.of(
                WorkOrderMaterial.builder().totalPrice(new BigDecimal("500.00")).build(),
                WorkOrderMaterial.builder().totalPrice(new BigDecimal("350.50")).build()
        );

        BigDecimal sum = engine.calculateTotalMaterialsCost(list);
        assertEquals(new BigDecimal("850.50"), sum);
    }
}
