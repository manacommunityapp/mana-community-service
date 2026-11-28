package com.manacommunity.api.testing.hardware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of parsing a serial binary frame through the chaos-hardened frame synchronizer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerialFrameResult {
    private boolean valid;
    private String statusMessage; // e.g. "SUCCESS", "CRC_MISMATCH", "MISSING_STX_OR_ETX", "BUFFER_UNDERFLOW"
    private RfidTagReadEvent tagEvent;
    private int discardedNoiseBytes;
}
