package com.manacommunity.api.unit.iot.metering;

import com.manacommunity.api.iot.metering.MeteringEnums.MeterType;
import com.manacommunity.api.iot.metering.engine.UtilityTierSlabEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UtilityTierSlabEngine Unit Tests")
public class UtilityTierSlabEngineTest {

    private UtilityTierSlabEngine engine;

    @BeforeEach
    void setUp() {
        engine = new UtilityTierSlabEngine();
    }

    @Test
    @DisplayName("Tiered electricity calculation (150 units = 100@6.0 + 50@8.5)")
    void testElectricitySlabCalculation() {
        BigDecimal bill = engine.calculateSlabAmount(MeterType.ELECTRICITY_METER, new BigDecimal("150.0"));
        // (100 * 6.0) + (50 * 8.5) = 600 + 425 = 1025.00
        assertEquals(new BigDecimal("1025.00"), bill);
    }

    @Test
    @DisplayName("Tiered water calculation (20,000 Liters = 20 kL -> 10@15 + 10@28)")
    void testWaterSlabCalculation() {
        BigDecimal bill = engine.calculateSlabAmount(MeterType.WATER_METER, new BigDecimal("20000.0"));
        // 20 kL: (10 * 15.0) + (10 * 28.0) = 150 + 280 = 430.00
        assertEquals(new BigDecimal("430.00"), bill);
    }

    @Test
    @DisplayName("Diesel generator flat backup power tariff (20 kWh @ ₹22)")
    void testDgPowerCalculation() {
        BigDecimal bill = engine.calculateSlabAmount(MeterType.DIESEL_GENERATOR, new BigDecimal("20.0"));
        assertEquals(new BigDecimal("440.00"), bill);
    }
}
