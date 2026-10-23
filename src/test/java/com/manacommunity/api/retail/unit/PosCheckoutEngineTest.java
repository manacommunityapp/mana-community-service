package com.manacommunity.api.retail.unit;

import com.manacommunity.api.retail.engine.PosCheckoutEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("POS Checkout & Billing Engine Unit Tests")
class PosCheckoutEngineTest {

    private PosCheckoutEngine posEngine;

    @BeforeEach
    void setUp() {
        posEngine = new PosCheckoutEngine();
    }

    @Test
    @DisplayName("Should calculate full bill summary with discounts and tax")
    void shouldCalculateFullBillSummary() {
        var item1 = new PosCheckoutEngine.PosLineItem(
                1L, "Organic Milk 1L", 2, new BigDecimal("60.00"), BigDecimal.ZERO, new BigDecimal("5.00") // 120 + 5% tax = 126
        );
        var item2 = new PosCheckoutEngine.PosLineItem(
                2L, "Artisan Bread", 1, new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("12.00") // 100 - 10 = 90 + 12% tax = 100.80
        );

        var summary = posEngine.computeBillSummary(List.of(item1, item2));

        assertEquals(new BigDecimal("220.00"), summary.subtotal());
        assertEquals(new BigDecimal("10.00"), summary.totalDiscount());
        assertEquals(new BigDecimal("210.00"), summary.taxableAmount());
        // Tax = 6.00 (milk) + 10.80 (bread) = 16.80
        assertEquals(new BigDecimal("16.80"), summary.totalTax());
        // Net = 210 + 16.80 = 226.80
        assertEquals(new BigDecimal("226.80"), summary.netPayable());
    }

    @Test
    @DisplayName("Should calculate tender change correctly")
    void shouldCalculateTenderChange() {
        BigDecimal change = posEngine.calculateTenderChange(new BigDecimal("226.80"), new BigDecimal("300.00"));
        assertEquals(new BigDecimal("73.20"), change);
    }

    @Test
    @DisplayName("Should throw exception if tender amount is insufficient")
    void shouldThrowExceptionWhenTenderInsufficient() {
        assertThrows(IllegalArgumentException.class, () ->
                posEngine.calculateTenderChange(new BigDecimal("200.00"), new BigDecimal("150.00")));
    }
}
