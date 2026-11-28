package com.manacommunity.api.edgehardware.service;

import com.manacommunity.api.edgehardware.entity.EdgeGatewayOtaState;
import com.manacommunity.api.edgehardware.repository.EdgeGatewayOtaStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Atomic A/B Partition OTA & 30-Second Watchdog Auto-Rollback Service:
 * - Verifies SHA-256 checksum + Ed25519 signature before staging onto inactive slot.
 * - Defers flash writes if cabin SoC temperature > 80C to protect hardware longevity.
 * - Automatically reverts to known-good slot if trial boot fails serial probe, TLS/MQTT reconnect, or watchdog within 30s.
 */
@Service
@RequiredArgsConstructor
public class EdgeOtaWatchdogService {

    public static final double MAX_FLASH_TEMP_CELSIUS = 80.0;
    public static final long WATCHDOG_RECONNECT_WINDOW_SEC = 30L;

    private final EdgeGatewayOtaStateRepository otaRepo;

    @Transactional
    public EdgeGatewayOtaState stageSignedFirmware(
            Long communityId,
            String controllerId,
            String newVersion,
            byte[] imageBytes,
            String expectedSha256Hex,
            byte[] ed25519Signature,
            PublicKey factoryPublicKey,
            double socTempCelsius
    ) {
        String normId = controllerId.trim().toUpperCase(Locale.ROOT);
        EdgeGatewayOtaState state = otaRepo.findByControllerId(normId)
                .orElseGet(() -> EdgeGatewayOtaState.builder()
                        .controllerId(normId)
                        .communityId(communityId)
                        .activeSlot("A")
                        .targetSlot("A")
                        .currentVersion("1.0.0")
                        .build());

        state.setSocTempCelsius(socTempCelsius);
        if (socTempCelsius > MAX_FLASH_TEMP_CELSIUS) {
            state.setOtaStatus("THERMAL_DEFERRED");
            state.setUpdatedAt(Instant.now());
            return otaRepo.save(state);
        }

        // Verify SHA-256 image integrity
        String actualSha = sha256Hex(imageBytes);
        if (!MessageDigest.isEqual(
                actualSha.toLowerCase(Locale.ROOT).getBytes(),
                expectedSha256Hex.trim().toLowerCase(Locale.ROOT).getBytes())) {
            throw new SecurityException("Firmware SHA-256 checksum mismatch");
        }

        // Verify Ed25519 cryptographic signature against immutable factory public key
        if (!verifyEd25519(factoryPublicKey, imageBytes, ed25519Signature)) {
            throw new SecurityException("Firmware Ed25519 cryptographic signature verification failed");
        }

        String inactiveSlot = "A".equalsIgnoreCase(state.getActiveSlot()) ? "B" : "A";
        state.setTargetSlot(inactiveSlot);
        state.setPendingVersion(newVersion);
        state.setUpgradeAvailable(true);
        state.setBootCount(0);
        state.setBootLimit(1);
        state.setOtaStatus("TRIAL_BOOTING");
        state.setUpdatedAt(Instant.now());
        return otaRepo.save(state);
    }

    @Transactional
    public EdgeGatewayOtaState evaluateTrialBootWatchdog(
            String controllerId,
            boolean serialBusHealthy,
            boolean tlsMqttHealthy,
            long secondsSinceBoot,
            boolean crashLoopDetected
    ) {
        String normId = controllerId.trim().toUpperCase(Locale.ROOT);
        EdgeGatewayOtaState state = otaRepo.findByControllerId(normId)
                .orElseThrow(() -> new IllegalArgumentException("Controller not found: " + controllerId));

        Instant now = Instant.now();
        if (!state.isUpgradeAvailable()) {
            state.setLastHeartbeatAt(now);
            state.setUpdatedAt(now);
            return otaRepo.save(state);
        }

        boolean failedWatchdog = crashLoopDetected
                || secondsSinceBoot > WATCHDOG_RECONNECT_WINDOW_SEC
                || !serialBusHealthy
                || !tlsMqttHealthy;

        if (failedWatchdog) {
            // Bootloader sees bootCount >= bootLimit with upgrade_available -> roll back to activeSlot
            state.setBootCount(state.getBootCount() + 1);
            state.setTargetSlot(state.getActiveSlot());
            state.setPendingVersion(null);
            state.setUpgradeAvailable(false);
            state.setOtaStatus("ROLLED_BACK");
            state.setUpdatedAt(now);
            return otaRepo.save(state);
        }

        // Healthy within 30s -> promote trial slot cleanly
        state.setActiveSlot(state.getTargetSlot());
        if (state.getPendingVersion() != null) {
            state.setCurrentVersion(state.getPendingVersion());
        }
        state.setPendingVersion(null);
        state.setUpgradeAvailable(false);
        state.setOtaStatus("HEALTHY");
        state.setLastHeartbeatAt(now);
        state.setUpdatedAt(now);
        return otaRepo.save(state);
    }

    public static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception ex) {
            throw new RuntimeException("SHA-256 unavailable", ex);
        }
    }

    public static boolean verifyEd25519(PublicKey publicKey, byte[] payload, byte[] signatureBytes) {
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(payload);
            return verifier.verify(signatureBytes);
        } catch (Exception ex) {
            return false;
        }
    }
}
