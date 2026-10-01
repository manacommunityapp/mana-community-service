package com.manacommunity.api.parking.anpr.dto;

import com.manacommunity.api.parking.anpr.entity.AnprGateEvent;

import java.time.LocalDateTime;

/** Used in the security dashboard / gate event list APIs. */
public record AnprGateEventResponse(
        Long id,
        String gateId,
        String direction,
        String plateNumber,
        Double confidence,
        String anprStatus,
        AnprGateEvent.BarrierAction barrierAction,
        String matchedResidentName,
        Long matchedVehicleId,
        Long matchedVisitorPassId,
        Double processingMs,
        LocalDateTime createdAt
) {
    public static AnprGateEventResponse from(AnprGateEvent e) {
        return new AnprGateEventResponse(
                e.getId(), e.getGateId(), e.getDirection(), e.getPlateNumber(),
                e.getConfidence(), e.getAnprStatus(), e.getBarrierAction(),
                e.getMatchedResidentName(), e.getMatchedVehicleId(),
                e.getMatchedVisitorPassId(), e.getProcessingMs(), e.getCreatedAt()
        );
    }
}
