package com.manacommunity.api.parking.ev.dto;

import com.manacommunity.api.parking.ev.enums.EvConnectorType;
import com.manacommunity.api.parking.ev.enums.EvStationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvStationResponse(
    Long id,
    String hardwareId,
    String name,
    Long communityId,
    Long parkingSlotId,
    EvConnectorType connectorType,
    BigDecimal maxPowerKw,
    EvStationStatus status,
    BigDecimal currentPowerKw,
    BigDecimal latestMeterKwh,
    LocalDateTime lastHeartbeatAt,
    String firmwareVersion,
    String lastFaultCode
) {}
