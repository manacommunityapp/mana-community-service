package com.manacommunity.api.testing.hardware;

import java.util.List;

/**
 * Deterministic hardware simulator and chaos harness for IoT serial & Modbus protocols.
 */
public interface SerialPacketChaosHarness {

    /**
     * Encodes an RFID read into a framed binary packet with standard CRC-16.
     */
    byte[] encodeRfidPacket(String tagHexId, int antennaId);

    /**
     * Chaos Injection: Inverts the CRC-16 checksum bytes to simulate line noise.
     */
    byte[] corruptCrc(byte[] frame);

    /**
     * Chaos Injection: Injects spurious noise bytes before the STX frame delimiter.
     */
    byte[] injectNoisePreamble(byte[] frame, byte[] noiseBytes);

    /**
     * Chaos Injection: Slices a contiguous packet into multiple fragments to test ring buffer reassembly.
     */
    List<byte[]> slicePacket(byte[] frame, int chunkSize);

    /**
     * Parses a raw byte stream through the frame-synchronizer, recovering from noise, verifying CRC-16.
     */
    SerialFrameResult parseSerialFrame(byte[] buffer);

    /**
     * Computes standard CRC-16/CCITT-FALSE ($x^{16} + x^{12} + x^5 + 1$, Init 0xFFFF).
     */
    int computeCrc16Ccitt(byte[] data, int offset, int length);

    /**
     * Simulates Modbus boom-barrier coil actuation with vehicle presence interlock and network timeout detection.
     */
    ModbusBarrierState executeBarrierCoilAction(ModbusBarrierState state, boolean openRequest, 
                                                long injectedLatencyMs, long timeoutThresholdMs);
}
