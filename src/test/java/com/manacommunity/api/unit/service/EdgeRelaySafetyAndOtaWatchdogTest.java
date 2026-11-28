package com.manacommunity.api.unit.service;

import com.manacommunity.api.edgehardware.entity.EdgeBarrierCommandNonce;
import com.manacommunity.api.edgehardware.entity.EdgeGatewayOtaState;
import com.manacommunity.api.edgehardware.repository.EdgeBarrierCommandNonceRepository;
import com.manacommunity.api.edgehardware.repository.EdgeGatewayOtaStateRepository;
import com.manacommunity.api.edgehardware.service.BarrierRelaySafetyService;
import com.manacommunity.api.edgehardware.service.BarrierRelaySafetyService.BarrierCommandRequest;
import com.manacommunity.api.edgehardware.service.EdgeOtaWatchdogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EdgeRelaySafetyAndOtaWatchdogTest {

    @Mock private EdgeBarrierCommandNonceRepository nonceRepo;
    @Mock private EdgeGatewayOtaStateRepository otaRepo;

    private BarrierRelaySafetyService relayService;
    private EdgeOtaWatchdogService otaService;

    private final Map<String, EdgeBarrierCommandNonce> nonceStore = new ConcurrentHashMap<>();
    private final Map<String, EdgeGatewayOtaState> otaStore = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        nonceStore.clear();
        otaStore.clear();

        lenient().when(nonceRepo.findByControllerIdAndCmdNonce(anyString(), anyString())).thenAnswer(inv ->
                Optional.ofNullable(nonceStore.get(inv.getArgument(0) + ":" + inv.getArgument(1))));
        lenient().when(nonceRepo.save(any())).thenAnswer(inv -> {
            EdgeBarrierCommandNonce n = inv.getArgument(0);
            if (n.getId() == null) n.setId(seq.getAndIncrement());
            nonceStore.put(n.getControllerId() + ":" + n.getCmdNonce(), n);
            return n;
        });

        lenient().when(otaRepo.findByControllerId(anyString())).thenAnswer(inv ->
                Optional.ofNullable(otaStore.get(inv.getArgument(0))));
        lenient().when(otaRepo.save(any())).thenAnswer(inv -> {
            EdgeGatewayOtaState s = inv.getArgument(0);
            if (s.getId() == null) s.setId(seq.getAndIncrement());
            otaStore.put(s.getControllerId(), s);
            return s;
        });

        relayService = new BarrierRelaySafetyService(nonceRepo);
        otaService = new EdgeOtaWatchdogService(otaRepo);
    }

    @Test
    void suppressesEightSecondDuplicateMqttDeliveryAndEnforcesLoopInterlocks() {
        Instant issued = Instant.parse("2026-11-28T08:05:00Z");
        var openReq = BarrierCommandRequest.builder()
                .controllerId("PI-GATE-01")
                .cmdNonce("cmd-uuid-001")
                .commandType("OPEN_ONLY")
                .loopAOccupied(true)
                .loopBOccupied(false)
                .issuedAt(issued)
                .ttlSeconds(15)
                .build();

        // First delivery triggers 500ms OPEN_ONLY relay pulse
        var first = relayService.executeBarrierCommand(openReq, issued.plusSeconds(1));
        assertThat(first.outcome()).isEqualTo("RELAY_PULSED");
        assertThat(first.relayPulseMs()).isEqualTo(500);
        assertThat(first.relayCoilTarget()).isEqualTo("COIL_OPEN_DRY_CONTACT");

        // 8s later MQTT QoS1 redelivery is deduplicated cleanly while Car #2 approaches Loop A
        var dup8s = relayService.executeBarrierCommand(openReq, issued.plusSeconds(8));
        assertThat(dup8s.outcome()).isEqualTo("IDEMPOTENT_DUPLICATE");
        assertThat(dup8s.relayPulseMs()).isZero();

        // Stale delayed open (>15s TTL) rejected without opening
        var stale = relayService.executeBarrierCommand(
                BarrierCommandRequest.builder()
                        .controllerId("PI-GATE-01").cmdNonce("cmd-uuid-002").commandType("OPEN_ONLY")
                        .loopAOccupied(true).loopBOccupied(false).issuedAt(issued).ttlSeconds(15).build(),
                issued.plusSeconds(20));
        assertThat(stale.outcome()).isEqualTo("STALE_COMMAND_EXPIRED");

        // Close command refused while vehicle mass sits over safety Loop B
        var dropPrevented = relayService.executeBarrierCommand(
                BarrierCommandRequest.builder()
                        .controllerId("PI-GATE-01").cmdNonce("cmd-close-1").commandType("CLOSE")
                        .loopAOccupied(false).loopBOccupied(true).issuedAt(issued).ttlSeconds(15).build(),
                issued.plusSeconds(4));
        assertThat(dropPrevented.outcome()).isEqualTo("LOOP_B_SAFETY_INHIBIT");
        assertThat(dropPrevented.hardwareInterlockEngaged()).isTrue();

        // Unauthorized entry across Loop A + Loop B raises TAILGATE_VIOLATION
        var tailgate = relayService.evaluateLoopTransition("PI-GATE-01", false, true, true, false);
        assertThat(tailgate.tailgateViolation()).isTrue();
        assertThat(tailgate.alertCode()).isEqualTo("TAILGATE_VIOLATION");
    }

    @Test
    void verifiesEd25519FirmwareHonorsThermalDeferralAndAutoRollsBackOn30sWatchdogTimeout() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();
        byte[] image = "mygate-edge-agent-v2.4.0-squashfs-payload".getBytes(StandardCharsets.UTF_8);
        String sha = EdgeOtaWatchdogService.sha256Hex(image);

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(kp.getPrivate());
        signer.update(image);
        byte[] validSig = signer.sign();

        // Thermal protection defers write when cabin SoC exceeds 80C
        EdgeGatewayOtaState hotDefer = otaService.stageSignedFirmware(
                412L, "PI-GATE-09", "2.4.0", image, sha, validSig, kp.getPublic(), 84.5);
        assertThat(hotDefer.getOtaStatus()).isEqualTo("THERMAL_DEFERRED");
        assertThat(hotDefer.isUpgradeAvailable()).isFalse();

        // Tampered image rejected cryptographically
        assertThatThrownBy(() -> otaService.stageSignedFirmware(
                412L, "PI-GATE-09", "2.4.0", "tampered".getBytes(StandardCharsets.UTF_8),
                sha, validSig, kp.getPublic(), 52.0))
                .isInstanceOf(SecurityException.class);

        // Valid signed image stages cleanly onto inactive Slot B with activeSlot remaining A
        EdgeGatewayOtaState staged = otaService.stageSignedFirmware(
                412L, "PI-GATE-09", "2.4.0", image, sha, validSig, kp.getPublic(), 52.0);
        assertThat(staged.getActiveSlot()).isEqualTo("A");
        assertThat(staged.getTargetSlot()).isEqualTo("B");
        assertThat(staged.isUpgradeAvailable()).isTrue();
        assertThat(staged.getOtaStatus()).isEqualTo("TRIAL_BOOTING");

        // Failure to reconnect TLS/MQTT within 30s triggers automatic rollback back to Slot A
        EdgeGatewayOtaState rolledBack = otaService.evaluateTrialBootWatchdog(
                "PI-GATE-09", true, false, 35L, false);
        assertThat(rolledBack.getOtaStatus()).isEqualTo("ROLLED_BACK");
        assertThat(rolledBack.getActiveSlot()).isEqualTo("A");
        assertThat(rolledBack.getTargetSlot()).isEqualTo("A");
        assertThat(rolledBack.isUpgradeAvailable()).isFalse();

        // Re-stage cleanly and commit promotion when healthy within 30s
        otaService.stageSignedFirmware(412L, "PI-GATE-09", "2.4.0", image, sha, validSig, kp.getPublic(), 50.0);
        EdgeGatewayOtaState promoted = otaService.evaluateTrialBootWatchdog(
                "PI-GATE-09", true, true, 12L, false);
        assertThat(promoted.getOtaStatus()).isEqualTo("HEALTHY");
        assertThat(promoted.getActiveSlot()).isEqualTo("B");
        assertThat(promoted.getCurrentVersion()).isEqualTo("2.4.0");
    }
}
