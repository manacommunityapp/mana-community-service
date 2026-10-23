package com.manacommunity.api.emergency.dto;

import java.time.LocalDateTime;

public record SosBroadcastAlert(
        Long incidentId,
        String emergencyType,
        String severity,
        String residentName,
        String residentPhone,
        String location,
        String status,
        boolean lockdownActive,
        LocalDateTime triggeredAt
) {}