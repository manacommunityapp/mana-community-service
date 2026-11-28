package com.manacommunity.api.testing.hardware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SerialPacketChaosHarnessTest {

    private SerialPacketChaosHarness harness;

    @BeforeEach
    void setUp() {
        harness = new SerialPacketChaosHarnessImpl();
    }

    @Test
    void testEncodeAndParseRfidFrameSuccess() {
        String tagHex = "E280699500004012";
        int antennaId = 1;

        byte[] frame = harness.encodeRfidPacket(tagHex, antennaId);
        assertNotNull(frame);
        assertEquals(15, frame.length);
        assertEquals(SerialPacketChaosHarnessImpl.STX, frame[0]);
        assertEquals(SerialPacketChaosHarnessImpl.ETX, frame[14]);

        SerialFrameResult result = harness.parseSerialFrame(frame);
        assertTrue(result.isValid());
        assertEquals("SUCCESS", result.getStatusMessage());
        assertNotNull(result.getTagEvent());
        assertEquals(tagHex, result.getTagEvent().getTagHexId());
        assertEquals(antennaId, result.getTagEvent().getAntennaId());
        assertEquals(0, result.getDiscardedNoiseBytes());
    }

    @Test
    void testCorruptedCrcRejection() {
        String tagHex = "E280699500004012";
        byte[] frame = harness.encodeRfidPacket(tagHex, 2);

        // Inject CRC corruption
        byte[] corruptedFrame = harness.corruptCrc(frame);

        SerialFrameResult result = harness.parseSerialFrame(corruptedFrame);
        assertFalse(result.isValid(), "Corrupted CRC frame must be rejected");
        assertTrue(result.getStatusMessage().contains("CRC_MISMATCH"));
        assertNull(result.getTagEvent());
    }

    @Test
    void testPreambleNoiseRecovery() {
        String tagHex = "A1B2C3D4E5F60718";
        byte[] frame = harness.encodeRfidPacket(tagHex, 1);

        // Inject 4 bytes of line noise before the STX byte
        byte[] noiseBytes = new byte[]{(byte) 0xFF, (byte) 0xAA, 0x00, 0x55};
        byte[] noisyStream = harness.injectNoisePreamble(frame, noiseBytes);

        assertEquals(19, noisyStream.length);

        SerialFrameResult result = harness.parseSerialFrame(noisyStream);
        assertTrue(result.isValid(), "Parser must synchronize on STX and discard noise");
        assertEquals("SUCCESS", result.getStatusMessage());
        assertEquals(4, result.getDiscardedNoiseBytes(), "Must report 4 noise bytes discarded");
        assertEquals(tagHex, result.getTagEvent().getTagHexId());
    }

    @Test
    void testModbusBarrierSafetyInterlock() {
        ModbusBarrierState barrier = ModbusBarrierState.builder()
                .barrierId("GATE_01_BARRIER")
                .openCoil(true)
                .closeCoil(false)
                .armAngleDegrees(90)
                .safetyLoopVehiclePresent(true) // Car is physically under the barrier arm!
                .manualOverrideActive(false)
                .build();

        // Attempt to CLOSE barrier while vehicle is present
        ModbusBarrierState updated = harness.executeBarrierCoilAction(barrier, false, 50, 2000);

        // Must abort close command and keep arm OPEN at 90 degrees
        assertTrue(updated.isOpenCoil(), "Safety loop must keep open coil active");
        assertFalse(updated.isCloseCoil(), "Close coil must not energize");
        assertEquals(90, updated.getArmAngleDegrees(), "Arm must remain vertical (90 deg)");
        assertFalse(updated.isManualOverrideActive());
    }

    @Test
    void testModbusBarrierTimeoutChaosTransitionsToManualOverride() {
        ModbusBarrierState barrier = ModbusBarrierState.builder()
                .barrierId("GATE_01_BARRIER")
                .openCoil(false)
                .closeCoil(true)
                .armAngleDegrees(0)
                .safetyLoopVehiclePresent(false)
                .manualOverrideActive(false)
                .build();

        // Simulate 3500ms network timeout when threshold is 2000ms
        ModbusBarrierState updated = harness.executeBarrierCoilAction(barrier, true, 3500, 2000);

        assertTrue(updated.isManualOverrideActive(), "Timeout must transition edge controller to manual override");
    }
}
