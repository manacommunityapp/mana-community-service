package com.manacommunity.api.parking.ev.unit;

import com.manacommunity.api.parking.ev.engine.EvChargingBillingEngine;
import com.manacommunity.api.parking.ev.entity.EvTariffConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EvChargingBillingEngine Unit Tests")
class EvChargingBillingEngineTest {

    private EvChargingBillingEngine engine;
    private EvTariffConfig tariff;

    @BeforeEach
    void setUp() {
        engine = new EvChargingBillingEngine();
        tariff = EvTariffConfig.builder()
                .communityId(1L)
                .baseRatePerKwh(new BigDecimal("10.00"))
                .peakHourRatePerKwh(new BigDecimal("15.00"))
                .peakHourStart(LocalTime.of(18, 0))
                .peakHourEnd(LocalTime.of(22, 0))
                .idlePenaltyPerMinute(new BigDecimal("2.00"))
                .gracePeriodMinutes(15)
                .build();
    }

    @Test
    @DisplayName("calculateBill: computes standard rate energy cost without idle penalty")
    void calculateBill_standardRate_noIdlePenalty() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 10, 0); // 10:00 AM (off-peak)
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 12, 0);

        BigDecimal initialMeter = new BigDecimal("100.000");
        BigDecimal finalMeter = new BigDecimal("125.000"); // 25.000 kWh

        EvChargingBillingEngine.BillBreakdown bill = engine.calculateBill(
                initialMeter, finalMeter, start, end, 10, tariff
        );

        assertThat(bill.energyKwh()).isEqualByComparingTo("25.000");
        assertThat(bill.energyCost()).isEqualByComparingTo("250.00"); // 25 kWh * Rs 10
        assertThat(bill.idlePenalty()).isEqualByComparingTo("0.00"); // 10 mins <= 15 mins grace
        assertThat(bill.totalCost()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("calculateBill: applies peak hour rate during peak window")
    void calculateBill_peakHourRate() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 19, 0); // 7:00 PM (peak)
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 21, 0);

        BigDecimal initialMeter = new BigDecimal("50.000");
        BigDecimal finalMeter = new BigDecimal("70.000"); // 20.000 kWh

        EvChargingBillingEngine.BillBreakdown bill = engine.calculateBill(
                initialMeter, finalMeter, start, end, 0, tariff
        );

        assertThat(bill.energyKwh()).isEqualByComparingTo("20.000");
        assertThat(bill.energyCost()).isEqualByComparingTo("300.00"); // 20 kWh * Rs 15
        assertThat(bill.totalCost()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("calculateBill: applies idle penalty after grace period")
    void calculateBill_withIdlePenalty() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 12, 0);

        BigDecimal initialMeter = new BigDecimal("10.000");
        BigDecimal finalMeter = new BigDecimal("20.000"); // 10.000 kWh

        // 45 minutes idle time. Grace = 15 mins -> Billable = 30 mins * Rs 2 = Rs 60
        EvChargingBillingEngine.BillBreakdown bill = engine.calculateBill(
                initialMeter, finalMeter, start, end, 45, tariff
        );

        assertThat(bill.energyKwh()).isEqualByComparingTo("10.000");
        assertThat(bill.energyCost()).isEqualByComparingTo("100.00");
        assertThat(bill.idlePenalty()).isEqualByComparingTo("60.00");
        assertThat(bill.totalCost()).isEqualByComparingTo("160.00");
    }
}
