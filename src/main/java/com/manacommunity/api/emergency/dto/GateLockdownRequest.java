package com.manacommunity.api.emergency.dto;

import com.manacommunity.api.emergency.entity.GateLockdownLog.LockdownDirective;

public record GateLockdownRequest(
        LockdownDirective directive,
        String affectedGates,
        String reason,
        Long incidentId
) {}