package com.manacommunity.api.parking.ev.unit;

import com.manacommunity.api.parking.ev.dto.EvTelemetryRequest;
import com.manacommunity.api.parking.ev.engine.EvTelemetryEngine;
import com.manacommunity.api.parking.ev.entity.EvChargingStation;
import com.manacommunity.api.parking.ev.entity.EvTelemetryHeartbeat;
import com.manacommunity.api.parking.ev.enums.EvStationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EvTelemetryEngine Unit Tests")
class EvTelemetryEngineTest {

    private EvTelemetryEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EvTelemetryEngine();
    }

    @Test
    @DisplayName("evaluateStatus: returns FAULTED when error code present")
    void evaluateStatus_withErrorCode_returnsFaulted() {
        EvChargingStation station = EvChargingStation.builder().id(1L).build();
        EvTelemetryRequest req = new EvTelemetryRequest(
                "STATION-01",
                new BigDecimal("120.500"),
                BigDecimal.ZERO,
                new BigDecimal("230.0"),
                BigDecimal.ZERO,
                new BigDecimal("35.0"),
                BigDecimal.ZERO,
                "ERR_OVER_CURRENT",
                LocalDateTime.now()
        );

        EvStationStatus status = engine.evaluateStatus(station, req, true);
        assertThat(status).isEqualTo(EvStationStatus.FAULTED);
    }

    @Test
    @DisplayName("evaluateStatus: returns CHARGING when active session and power > 0.05 kW")
    void evaluateStatus_withActiveSessionAndPower_returnsCharging() {
        EvChargingStation station = EvChargingStation.builder().id(1L).build();
        EvTelemetryRequest req = new EvTelemetryRequest(
                "STATION-01",
                new BigDecimal("120.500"),
                new BigDecimal("7.20"),
                new BigDecimal("230.0"),
                new BigDecimal("31.3"),
                new BigDecimal("38.5"),
                new BigDecimal("65.0"),
                null,
                LocalDateTime.now()
        );

        EvStationStatus status = engine.evaluateStatus(station, req, true);
        assertThat(status).isEqualTo(EvStationStatus.CHARGING);
    }

    @Test
    @DisplayName("evaluateStatus: returns OCCUPIED_IDLE when active session but power near zero")
    void evaluateStatus_withActiveSessionAndZeroPower_returnsOccupiedIdle() {
        EvChargingStation station = EvChargingStation.builder().id(1L).build();
        EvTelemetryRequest req = new EvTelemetryRequest(
                "STATION-01",
                new BigDecimal("150.000"),
                BigDecimal.ZERO,
                new BigDecimal("230.0"),
                BigDecimal.ZERO,
                new BigDecimal("30.0"),
                new BigDecimal("100.0"),
                null,
                LocalDateTime.now()
        );

        EvStationStatus status = engine.evaluateStatus(station, req, true);
        assertThat(status).isEqualTo(EvStationStatus.OCCUPIED_IDLE);
    }

    @Test
    @DisplayName("toHeartbeatEntity: correctly transforms DTO into Heartbeat entity")
    void toHeartbeatEntity_transformsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        EvTelemetryRequest req = new EvTelemetryRequest(
                "STATION-01",
                new BigDecimal("200.123"),
                new BigDecimal("11.00"),
                new BigDecimal("400.0"),
                new BigDecimal("16.0"),
                new BigDecimal("42.0"),
                new BigDecimal("80.0"),
                null,
                now
        );

        EvTelemetryHeartbeat hb = engine.toHeartbeatEntity(10L, 100L, req);
        assertThat(hb.getStationId()).isEqualTo(10L);
        assertThat(hb.getSessionId()).isEqualTo(100L);
        assertThat(hb.getMeterReadingKwh()).isEqualByComparingTo("200.123");
        assertThat(hb.getPowerKw()).isEqualByComparingTo("11.00");
        assertThat(hb.getVoltage()).isEqualByComparingTo("400.0");
        assertThat(hb.getStateOfChargePercent()).isEqualByComparingTo("80.0");
    }
}
