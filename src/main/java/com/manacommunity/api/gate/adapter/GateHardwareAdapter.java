package com.manacommunity.api.gate.adapter;

/**
 * Unified abstraction over all gate hardware types (ANPR, RFID, SmartLock, BoomBarrier).
 *
 * Each implementation handles one {@link HardwareType}. The {@link
 * com.manacommunity.api.gate.service.GateHardwareDispatcher} routes incoming events to the
 * matching adapter, persists the result, and broadcasts it over STOMP.
 */
public interface GateHardwareAdapter {

    HardwareType hardwareType();

    /**
     * Process an inbound hardware event (plate read, RFID swipe, etc.) and return
     * a barrier-action decision plus audit metadata.
     */
    GateEventResult process(RawHardwareEvent event);

    /**
     * Push an outbound barrier command (open, close, lock, unlock) to the hardware.
     * Implementations that cannot receive commands should return a no-op result.
     */
    BarrierCommandResult sendCommand(BarrierCommand command);
}
