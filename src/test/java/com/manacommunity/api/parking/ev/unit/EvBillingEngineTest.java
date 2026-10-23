package com.manacommunity.api.parking.ev.unit;

import com.manacommunity.api.parking.ev.engine.EvBillingEngine;
import com.manacommunity.api.parking.ev.entity.EvChargingRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EvBillingEngine Unit Tests")
class EvBillingEngineTest {

    private EvBillingEngine engine;
    private EvChargingRate rateConfig;

    @BeforeEach
    void setUp() {
        engine = new EvBillingEngine();
        rateConfig = EvChargingRate.builder()
                .unitRatePerKwh(new BigDecimal("10.00"))
                .peakRatePerKwh(new BigDecimal("15.00"))
                .peakStartHour(18)
                .peakEndHour(22)
                .minimumBillAmount(new BigDecimal("30.00"))
                .build();
    }

    @Test
    @DisplayName("calculateKwh: computes net units correctly and protects against rollbacks")
    void calculateKwh_computesCorrectly() {
        assertThat(engine.calculateKwh(100.0, 125.5)).isEqualTo(25.5);
        assertThat(engine.calculateKwh(125.5, 100.0)).isEqualTo(0.0);
        assertThat(engine.calculateKwh(null, 100.0)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateCost: standard rate off-peak computation")
    void calculateCost_standardRate() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 10, 0); // 10:00 AM (off-peak)
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 12, 0);

        BigDecimal cost = engine.calculateCost(10.0, start, end, rateConfig);
        assertThat(cost).isEqualByComparingTo("100.00"); // 10 kWh * Rs 10 = Rs 100 (> min Rs 30)
    }

    @Test
    @DisplayName("calculateCost: peak rate computation")
    void calculateCost_peakRate() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 19, 0); // 7:00 PM (peak)
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 21, 0);

        BigDecimal cost = engine.calculateCost(10.0, start, end, rateConfig);
        assertThat(cost).isEqualByComparingTo("150.00"); // 10 kWh * Rs 15 = Rs 150
    }

    @Test
    @DisplayName("calculateCost: applies minimum bill threshold")
    void calculateCost_minimumThreshold() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 10, 15);

        BigDecimal cost = engine.calculateCost(1.5, start, end, rateConfig); // 1.5 kWh * Rs 10 = Rs 15 < Rs 30
        assertThat(cost).isEqualByComparingTo("30.00");
    }
}
