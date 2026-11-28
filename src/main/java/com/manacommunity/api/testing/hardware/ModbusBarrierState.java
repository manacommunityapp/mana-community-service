package com.manacommunity.api.testing.hardware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the state of a Modbus-TCP boom barrier edge controller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModbusBarrierState {
    private String barrierId;           // e.g. "BOOM_BARRIER_GATE_1"
    private boolean openCoil;           // Coil 0x0001
    private boolean closeCoil;          // Coil 0x0002
    private boolean safetyLoopVehiclePresent; // Discrete Input 0x1001 (1 = vehicle present under arm)
    private int armAngleDegrees;        // Input Register 0x3001 (0 = closed horizontal, 90 = open vertical)
    private boolean manualOverrideActive; // Failsafe state if command timed out
}
