package com.manacommunity.api.cfbos.unit.charge;

import com.manacommunity.api.cfbos.charge.dto.ChargeCalculationResult;
import com.manacommunity.api.cfbos.charge.dto.PropertyContext;
import com.manacommunity.api.cfbos.charge.engine.ChargeCalculationEngine;
import com.manacommunity.api.cfbos.charge.entity.TierConfig;
import com.manacommunity.api.cfbos.charge.enums.CalculationMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChargeCalculationEngine")
class ChargeCalculationEngineTest {

    private final ChargeCalculationEngine engine = new ChargeCalculationEngine();

    @Nested
    @DisplayName("Fixed amount")
    class FixedAmount {
        @Test
        @DisplayName("should return fixed amount regardless of property context")
        void shouldReturnFixedAmount() {
            PropertyContext ctx = PropertyContext.builder().area(new BigDecimal("1200")).build();
            ChargeCalculationResult result = engine.calculate(
                    CalculationMethod.FIXED, new BigDecimal("3500.00"), null, ctx);
            assertThat(result.getAmount()).isEqualByComparingTo("3500.00");
        }
    }

    @Nested
    @DisplayName("Area based")
    class AreaBased {
        @Test
        @DisplayName("should calculate area × rate per unit")
        void shouldCalculateAreaBased() {
            PropertyContext ctx = PropertyContext.builder().area(new BigDecimal("1200")).build();
            ChargeCalculationResult result = engine.calculate(
                    CalculationMethod.AREA_BASED, null, new BigDecimal("4.50"), ctx);
            assertThat(result.getAmount()).isEqualByComparingTo("5400.00");
        }
    }

    @Nested
    @DisplayName("Consumption based")
    class ConsumptionBased {
        @Test
        @DisplayName("should calculate consumption × rate")
        void shouldCalculateConsumptionBased() {
            PropertyContext ctx = PropertyContext.builder().consumption(new BigDecimal("15")).build();
            ChargeCalculationResult result = engine.calculate(
                    CalculationMethod.CONSUMPTION_BASED, null, new BigDecimal("40.00"), ctx);
            assertThat(result.getAmount()).isEqualByComparingTo("600.00");
        }
    }

    @Nested
    @DisplayName("Slab based")
    class SlabBased {
        @Test
        @DisplayName("should calculate tiered pricing")
        void shouldCalculateSlabPricing() {
            List<TierConfig> tiers = List.of(
                    TierConfig.builder().tierFrom(BigDecimal.ZERO).tierTo(new BigDecimal("10"))
                            .rate(new BigDecimal("20.00")).tierOrder(1).build(),
                    TierConfig.builder().tierFrom(new BigDecimal("10")).tierTo(new BigDecimal("20"))
                            .rate(new BigDecimal("35.00")).tierOrder(2).build(),
                    TierConfig.builder().tierFrom(new BigDecimal("20")).tierTo(null)
                            .rate(new BigDecimal("55.00")).tierOrder(3).build()
            );
            ChargeCalculationResult result = engine.calculateSlab(tiers, new BigDecimal("25"));
            // 10*20 + 10*35 + 5*55 = 200 + 350 + 275 = 825
            assertThat(result.getAmount()).isEqualByComparingTo("825.00");
        }
    }

    @Nested
    @DisplayName("Formula based")
    class FormulaBased {
        @Test
        @DisplayName("should evaluate SpEL formula")
        void shouldEvaluateFormula() {
            BigDecimal result = engine.evaluateFormula(
                    "(area * ratePerSqft) + (floor > 5 ? 500 : 0)",
                    Map.of("area", 1200, "ratePerSqft", 4.5, "floor", 8)
            );
            // 1200 * 4.5 + 500 = 5900
            assertThat(result).isEqualByComparingTo("5900");
        }
    }
}
