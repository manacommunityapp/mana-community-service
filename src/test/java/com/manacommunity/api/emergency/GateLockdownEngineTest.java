package com.manacommunity.api.emergency;

import com.manacommunity.api.emergency.engine.GateLockdownEngine;
import com.manacommunity.api.emergency.entity.GateLockdownLog.LockdownDirective;
import com.manacommunity.api.emergency.entity.SosIncident.EmergencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gate Lockdown & Safety Engine Unit Tests")
public class GateLockdownEngineTest {

    private final GateLockdownEngine engine = new GateLockdownEngine();

    @Test
    @DisplayName("Should trigger LOCKDOWN_CLOSE_ALL on Security Intruder or Theft")
    void shouldLockdownOnIntruder() {
        assertThat(engine.evaluateDirective(EmergencyType.SECURITY_INTRUDER))
                .isEqualTo(LockdownDirective.LOCKDOWN_CLOSE_ALL);
        assertThat(engine.evaluateDirective(EmergencyType.THEFT))
                .isEqualTo(LockdownDirective.LOCKDOWN_CLOSE_ALL);
    }

    @Test
    @DisplayName("Should trigger EVACUATION_OPEN_ALL on Fire or Gas Leak")
    void shouldEvacuateOnFire() {
        assertThat(engine.evaluateDirective(EmergencyType.FIRE))
                .isEqualTo(LockdownDirective.EVACUATION_OPEN_ALL);
        assertThat(engine.evaluateDirective(EmergencyType.GAS_LEAK))
                .isEqualTo(LockdownDirective.EVACUATION_OPEN_ALL);
    }

    @Test
    @DisplayName("Should prioritize emergency service access for Medical and Fire")
    void shouldPrioritizeEmergencyServices() {
        assertThat(engine.shouldPrioritizeEmergencyServices(EmergencyType.MEDICAL)).isTrue();
        assertThat(engine.shouldPrioritizeEmergencyServices(EmergencyType.FIRE)).isTrue();
        assertThat(engine.shouldPrioritizeEmergencyServices(EmergencyType.SECURITY_INTRUDER)).isFalse();
    }
}