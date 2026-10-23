package com.manacommunity.api.unit.iot.metering;

import com.manacommunity.api.iot.metering.MeteringEnums.*;
import com.manacommunity.api.iot.metering.SmartMeter;
import com.manacommunity.api.iot.metering.engine.MeterPulseIngestionEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MeterPulseIngestionEngine Unit Tests")
public class MeterPulseIngestionEngineTest {

    private MeterPulseIngestionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MeterPulseIngestionEngine();
    }

    @Test
    @DisplayName("Normal electricity pulse ingestion computes delta and cumulative reading")
    void testNormalElectricityPulse() {
        SmartMeter meter = SmartMeter.builder()
                .meterSerialNumber("EM-T1-101")
                .meterType(MeterType.ELECTRICITY_METER)
                .pulseMultiplier(new BigDecimal("0.5")) // 0.5 kWh per pulse
                .lastPulseCount(100L)
                .lastReading(new BigDecimal("50.0"))
                .status(MeterStatus.ACTIVE)
                .build();

        var result = engine.processPulse(meter, 120L, new BigDecimal("4.2"), false);

        assertFalse(result.anomalyDetected());
        assertEquals(new BigDecimal("10.0000"), result.deltaConsumed()); // (120 - 100) * 0.5 = 10 kWh
        assertEquals(new BigDecimal("60.0000"), result.newCumulativeReading());
        assertEquals(MeterStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("Meter rollback triggers tamper status alert")
    void testMeterRollbackTamper() {
        SmartMeter meter = SmartMeter.builder()
                .meterSerialNumber("WM-B2-303")
                .meterType(MeterType.WATER_METER)
                .pulseMultiplier(BigDecimal.ONE)
                .lastPulseCount(500L)
                .lastReading(new BigDecimal("500.0"))
                .status(MeterStatus.ACTIVE)
                .build();

        // New pulse count is 400 (less than 500)
        var result = engine.processPulse(meter, 400L, null, false);

        assertTrue(result.anomalyDetected());
        assertEquals(MeterStatus.TAMPERED, result.status());
        assertTrue(result.alertMessage().contains("rollback"));
    }

    @Test
    @DisplayName("Excessive water draw triggers leak alert flag")
    void testWaterLeakDetection() {
        SmartMeter meter = SmartMeter.builder()
                .meterSerialNumber("WM-T3-404")
                .meterType(MeterType.WATER_METER)
                .pulseMultiplier(BigDecimal.ONE)
                .lastPulseCount(1000L)
                .lastReading(new BigDecimal("1000.0"))
                .status(MeterStatus.ACTIVE)
                .build();

        // 3500 L drawn in interval (> 2000 L threshold)
        var result = engine.processPulse(meter, 4500L, null, false);

        assertTrue(result.anomalyDetected());
        assertTrue(result.leakFlag());
        assertTrue(result.alertMessage().contains("Excessive water flow"));
    }
}
