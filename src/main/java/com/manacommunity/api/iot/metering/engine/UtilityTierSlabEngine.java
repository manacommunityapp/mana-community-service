package com.manacommunity.api.iot.metering.engine;

import com.manacommunity.api.iot.metering.MeteringEnums.MeterType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class UtilityTierSlabEngine {

    /**
     * Computes bill amount based on tiered slab pricing.
     *
     * Electricity (per kWh):
     *   - 0 to 100 units: ₹6.00
     *   - 101 to 300 units: ₹8.50
     *   - > 300 units: ₹11.00
     *
     * Water (per Kiloliter / kL = 1000 Liters):
     *   - 0 to 10 kL: ₹15.00
     *   - 11 to 25 kL: ₹28.00
     *   - > 25 kL: ₹50.00
     *
     * Diesel Generator (DG Backup, per kWh):
     *   - Flat ₹22.00 / kWh
     */
    public BigDecimal calculateSlabAmount(MeterType type, BigDecimal unitsConsumed) {
        if (unitsConsumed == null || unitsConsumed.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        double units = unitsConsumed.doubleValue();
        double totalCost = 0.0;

        switch (type) {
            case ELECTRICITY_METER -> {
                if (units <= 100) {
                    totalCost = units * 6.00;
                } else if (units <= 300) {
                    totalCost = (100 * 6.00) + ((units - 100) * 8.50);
                } else {
                    totalCost = (100 * 6.00) + (200 * 8.50) + ((units - 300) * 11.00);
                }
            }
            case WATER_METER -> {
                // units are in Liters, convert to kL
                double kL = units / 1000.0;
                if (kL <= 10) {
                    totalCost = kL * 15.00;
                } else if (kL <= 25) {
                    totalCost = (10 * 15.00) + ((kL - 10) * 28.00);
                } else {
                    totalCost = (10 * 15.00) + (15 * 28.00) + ((kL - 25) * 50.00);
                }
            }
            case DIESEL_GENERATOR -> {
                totalCost = units * 22.00;
            }
            case GAS_METER -> {
                totalCost = units * 45.00;
            }
        }

        return BigDecimal.valueOf(totalCost).setScale(2, RoundingMode.HALF_UP);
    }
}
