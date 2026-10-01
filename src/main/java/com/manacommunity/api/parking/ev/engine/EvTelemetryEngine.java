package com.manacommunity.api.parking.ev.engine;

import com.manacommunity.api.parking.ev.dto.EvTelemetryRequest;
import com.manacommunity.api.parking.ev.entity.EvChargingStation;
import com.manacommunity.api.parking.ev.entity.EvTelemetryHeartbeat;
import com.manacommunity.api.parking.ev.enums.EvStationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class EvTelemetryEngine {

    /**
     * Updates station state from incoming telemetry.
     */
    public EvStationStatus evaluateStatus(EvChargingStation station, EvTelemetryRequest request, boolean hasActiveSession) {
        if (request.errorCode() != null && !request.errorCode().isBlank()) {
            return EvStationStatus.FAULTED;
        }

        BigDecimal power = request.powerKw() != null ? request.powerKw() : BigDecimal.ZERO;

        if (hasActiveSession) {
            if (power.compareTo(new BigDecimal("0.05")) > 0) {
                return EvStationStatus.CHARGING;
            } else {
                // Session is active but power is near zero -> Full charge or idle
                return EvStationStatus.OCCUPIED_IDLE;
            }
        }

        return EvStationStatus.AVAILABLE;
    }

    /**
     * Converts telemetry request to entity.
     */
    public EvTelemetryHeartbeat toHeartbeatEntity(Long stationId, Long sessionId, EvTelemetryRequest req) {
        return EvTelemetryHeartbeat.builder()
                .stationId(stationId)
                .sessionId(sessionId)
                .timestamp(req.timestamp() != null ? req.timestamp() : LocalDateTime.now())
                .powerKw(req.powerKw() != null ? req.powerKw() : BigDecimal.ZERO)
                .voltage(req.voltage())
                .currentAmps(req.currentAmps())
                .temperatureCelsius(req.temperatureCelsius())
                .meterReadingKwh(req.meterReadingKwh())
                .stateOfChargePercent(req.stateOfChargePercent())
                .errorCode(req.errorCode())
                .build();
    }
}
