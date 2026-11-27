package com.manacommunity.api.testing.hardware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class SerialPacketChaosHarnessImpl implements SerialPacketChaosHarness {

    public static final byte STX = 0x02; // Start of text
    public static final byte ETX = 0x03; // End of text
    public static final byte CMD_TAG_READ = (byte) 0x81;

    @Override
    public byte[] encodeRfidPacket(String tagHexId, int antennaId) {
        byte[] tagBytes = hexStringToByteArray(tagHexId);
        if (tagBytes.length != 8) {
            // Pad or truncate to 8 bytes for deterministic fixed-size framing
            tagBytes = Arrays.copyOf(tagBytes, 8);
        }

        int frameLength = 1 + 1 + 1 + 1 + tagBytes.length + 2 + 1; // 15 bytes
        byte[] frame = new byte[frameLength];

        frame[0] = STX;
        frame[1] = (byte) frameLength;
        frame[2] = CMD_TAG_READ;
        frame[3] = (byte) (antennaId & 0xFF);
        System.arraycopy(tagBytes, 0, frame, 4, tagBytes.length);

        // CRC-16 covers bytes from Length (index 1) through end of TagBytes (index 11)
        int crcLength = 1 + 1 + 1 + tagBytes.length; // 11 bytes
        int crc = computeCrc16Ccitt(frame, 1, crcLength);

        frame[frameLength - 3] = (byte) ((crc >> 8) & 0xFF); // High byte
        frame[frameLength - 2] = (byte) (crc & 0xFF);        // Low byte
        frame[frameLength - 1] = ETX;

        return frame;
    }

    @Override
    public byte[] corruptCrc(byte[] frame) {
        if (frame == null || frame.length < 4) {
            return frame;
        }
        byte[] corrupted = Arrays.copyOf(frame, frame.length);
        // Invert the CRC high byte (2 bytes before ETX)
        corrupted[corrupted.length - 3] ^= (byte) 0xFF;
        return corrupted;
    }

    @Override
    public byte[] injectNoisePreamble(byte[] frame, byte[] noiseBytes) {
        if (noiseBytes == null || noiseBytes.length == 0) {
            return frame;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(noiseBytes, 0, noiseBytes.length);
        baos.write(frame, 0, frame.length);
        return baos.toByteArray();
    }

    @Override
    public List<byte[]> slicePacket(byte[] frame, int chunkSize) {
        List<byte[]> slices = new ArrayList<>();
        if (frame == null || frame.length == 0 || chunkSize <= 0) {
            return slices;
        }
        int offset = 0;
        while (offset < frame.length) {
            int length = Math.min(chunkSize, frame.length - offset);
            byte[] slice = new byte[length];
            System.arraycopy(frame, offset, slice, 0, length);
            slices.add(slice);
            offset += length;
        }
        return slices;
    }

    @Override
    public SerialFrameResult parseSerialFrame(byte[] buffer) {
        if (buffer == null || buffer.length < 15) {
            return SerialFrameResult.builder()
                    .valid(false)
                    .statusMessage("BUFFER_UNDERFLOW: Insufficient bytes for minimum frame")
                    .build();
        }

        // 1. Frame Synchronization: Scan for STX (0x02)
        int stxIndex = -1;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == STX) {
                stxIndex = i;
                break;
            }
        }

        if (stxIndex == -1) {
            return SerialFrameResult.builder()
                    .valid(false)
                    .statusMessage("MISSING_STX: No start of text delimiter found")
                    .discardedNoiseBytes(buffer.length)
                    .build();
        }

        int discardedNoise = stxIndex;

        // 2. Length check
        if (stxIndex + 1 >= buffer.length) {
            return SerialFrameResult.builder()
                    .valid(false)
                    .statusMessage("INCOMPLETE_FRAME_HEADER")
                    .discardedNoiseBytes(discardedNoise)
                    .build();
        }

        int declaredLength = buffer[stxIndex + 1] & 0xFF;
        if (declaredLength != 15) {
            return SerialFrameResult.builder()
                    .valid(false)
                    .statusMessage("INVALID_FRAME_LENGTH: Declared length " + declaredLength + " != 15")
                    .discardedNoiseBytes(discardedNoise)
                    .build();
        }

        if (stxIndex + declaredLength > buffer.length) {
            return SerialFrameResult.builder()
                    .valid(false)
                    .statusMessage("PARTIAL_FRAME: Declared bytes not fully received")
                    .discardedNoiseBytes(discardedNoise)
                    .build();
        }

        // 3. ETX check
        int etxIndex = stxIndex + declaredLength - 1;
        if (buffer[etxIndex] != ETX) {
            return SerialFrameResult.builder()
                    .valid(false)
                    .statusMessage("MISSING_ETX: End delimiter missing at index " + etxIndex)
                    .discardedNoiseBytes(discardedNoise)
                    .build();
        }

        // 4. Verify CRC-16
        int expectedCrc = ((buffer[etxIndex - 2] & 0xFF) << 8) | (buffer[etxIndex - 1] & 0xFF);
        int computedCrc = computeCrc16Ccitt(buffer, stxIndex + 1, declaredLength - 4);

        if (expectedCrc != computedCrc) {
            return SerialFrameResult.builder()
                    .valid(false)
                    .statusMessage(String.format("CRC_MISMATCH: Computed=0x%04X, Received=0x%04X", computedCrc, expectedCrc))
                    .discardedNoiseBytes(discardedNoise)
                    .build();
        }

        // 5. Extract fields
        int antennaId = buffer[stxIndex + 3] & 0xFF;
        byte[] tagBytes = new byte[8];
        System.arraycopy(buffer, stxIndex + 4, tagBytes, 0, 8);
        String tagHex = byteArrayToHexString(tagBytes);

        RfidTagReadEvent event = RfidTagReadEvent.builder()
                .tagHexId(tagHex)
                .antennaId(antennaId)
                .signalStrengthRssi(-65)
                .timestampEpochMs(System.currentTimeMillis())
                .build();

        return SerialFrameResult.builder()
                .valid(true)
                .statusMessage("SUCCESS")
                .tagEvent(event)
                .discardedNoiseBytes(discardedNoise)
                .build();
    }

    @Override
    public int computeCrc16Ccitt(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= (data[offset + i] & 0xFF) << 8;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x8000) != 0) {
                    crc = ((crc << 1) ^ 0x1021) & 0xFFFF;
                } else {
                    crc = (crc << 1) & 0xFFFF;
                }
            }
        }
        return crc & 0xFFFF;
    }

    @Override
    public ModbusBarrierState executeBarrierCoilAction(ModbusBarrierState state, boolean openRequest, 
                                                       long injectedLatencyMs, long timeoutThresholdMs) {
        if (state == null) {
            throw new IllegalArgumentException("ModbusBarrierState cannot be null");
        }

        // 1. Edge Timeout Check (Toxiproxy delay simulation)
        if (injectedLatencyMs > timeoutThresholdMs) {
            log.error("Modbus TCP communication timeout: Injected {}ms exceeds threshold {}ms. Switching to MANUAL_OVERRIDE",
                    injectedLatencyMs, timeoutThresholdMs);
            state.setManualOverrideActive(true);
            return state;
        }

        // 2. Safety Interlock: If trying to CLOSE but vehicle loop detector is active, ABORT close
        if (!openRequest && state.isSafetyLoopVehiclePresent()) {
            log.warn("Safety loop detector active (vehicle present under barrier). Aborting CLOSE command to prevent vehicle collision!");
            state.setCloseCoil(false);
            state.setOpenCoil(true);
            state.setArmAngleDegrees(90); // Keep open
            return state;
        }

        // 3. Normal actuation
        if (openRequest) {
            state.setOpenCoil(true);
            state.setCloseCoil(false);
            state.setArmAngleDegrees(90); // Vertical open
            state.setManualOverrideActive(false);
        } else {
            state.setOpenCoil(false);
            state.setCloseCoil(true);
            state.setArmAngleDegrees(0);  // Horizontal closed
            state.setManualOverrideActive(false);
        }

        return state;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
