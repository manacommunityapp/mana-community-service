package com.manacommunity.api.emergency.dto;

import com.manacommunity.api.emergency.entity.SosIncident.IncidentStatus;

public record SosResolveRequest(
        IncidentStatus status, // RESOLVED or FALSE_ALARM
        String resolutionNotes,
        Boolean restoreGatesToNormal
) {}