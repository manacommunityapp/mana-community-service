package com.manacommunity.api.testing.hardware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parsed RFID UHF tag reading from RS-485 serial stream.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfidTagReadEvent {
    private String tagHexId;      // e.g. "E280699500004012"
    private int antennaId;        // e.g. 1 (Lane In), 2 (Lane Out)
    private int signalStrengthRssi; // e.g. -65 dBm
    private long timestampEpochMs;
}
