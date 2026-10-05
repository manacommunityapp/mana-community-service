package com.manacommunity.api.emergency.engine;

import com.manacommunity.api.emergency.entity.GateLockdownLog.LockdownDirective;
import com.manacommunity.api.emergency.entity.SosIncident.EmergencyType;
import org.springframework.stereotype.Component;

@Component
public class GateLockdownEngine {

    /**
     * Recommends automated gate barrier action based on emergency type.
     */
    public LockdownDirective evaluateDirective(EmergencyType type) {
        if (type == null) return LockdownDirective.NORMAL_RESTORE;

        return switch (type) {
            case FIRE, GAS_LEAK -> LockdownDirective.EVACUATION_OPEN_ALL;
            case SECURITY_INTRUDER, THEFT -> LockdownDirective.LOCKDOWN_CLOSE_ALL;
            case MEDICAL, LIFT_STUCK, GENERAL_PANIC -> LockdownDirective.NORMAL_RESTORE;
        };
    }

    /**
     * Checks if emergency type warrants automatic emergency vehicle bypass on all gates.
     */
    public boolean shouldPrioritizeEmergencyServices(EmergencyType type) {
        return type == EmergencyType.FIRE || type == EmergencyType.MEDICAL || type == EmergencyType.GAS_LEAK;
    }
}