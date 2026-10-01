package com.manacommunity.api.emergency.dto;

import com.manacommunity.api.emergency.entity.SosIncident;

import java.time.LocalDateTime;

public record SosIncidentResponse(
        Long id,
        Long communityId,
        Long residentId,
        String residentName,
        String residentPhone,
        String flatNumber,
        String buildingBlock,
        String emergencyType,
        String severity,
        String status,
        Double latitude,
        Double longitude,
        String locationDetails,
        String notes,
        boolean lockdownInitiated,
        int slaTargetSeconds,
        LocalDateTime triggeredAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime arrivedAt,
        LocalDateTime resolvedAt,
        String resolutionNotes
) {
    public static SosIncidentResponse from(SosIncident i) {
        return new SosIncidentResponse(
                i.getId(),
                i.getCommunity().getId(),
                i.getResident().getId(),
                i.getResident().getFullName(),
                i.getResident().getPhone(),
                i.getFlatNumber(),
                i.getBuildingBlock(),
                i.getEmergencyType().name(),
                i.getSeverity().name(),
                i.getStatus().name(),
                i.getLatitude(),
                i.getLongitude(),
                i.getLocationDetails(),
                i.getNotes(),
                i.isLockdownInitiated(),
                i.getSlaTargetSeconds(),
                i.getTriggeredAt(),
                i.getAcknowledgedAt(),
                i.getArrivedAt(),
                i.getResolvedAt(),
                i.getResolutionNotes()
        );
    }
}