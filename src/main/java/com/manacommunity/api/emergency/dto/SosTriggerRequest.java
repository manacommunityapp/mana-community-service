package com.manacommunity.api.emergency.dto;

import com.manacommunity.api.emergency.entity.SosIncident.EmergencyType;
import com.manacommunity.api.emergency.entity.SosIncident.Severity;

public record SosTriggerRequest(
        EmergencyType emergencyType,
        Severity severity,
        String flatNumber,
        String buildingBlock,
        Double latitude,
        Double longitude,
        String locationDetails,
        String notes,
        Boolean triggerGateLockdown
) {}