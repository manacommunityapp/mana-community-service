package com.manacommunity.api.unit.service;

import com.manacommunity.api.access.biometric.*;
import com.manacommunity.api.devices.entity.DeviceVisitorOtp;
import com.manacommunity.api.devices.entity.SmartDevice;
import com.manacommunity.api.devices.repository.DeviceVisitorOtpRepository;
import com.manacommunity.api.devices.repository.SmartDeviceRepository;
import com.manacommunity.api.devices.service.OfflineSmartLockPinService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartLockBiometricEdgeAndApbTest {

    @Mock private SmartDeviceRepository smartDeviceRepo;
    @Mock private DeviceVisitorOtpRepository deviceOtpRepo;

    @Mock private BiometricUserEnrollmentRepository enrollmentRepo;
    @Mock private BiometricEdgeDeltaRepository deltaRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @Mock private TurnstileApbStateRepository apbRepo;

    private OfflineSmartLockPinService offlinePinService;
    private BiometricEdgeSyncService edgeSyncService;
    private AntiPassbackService antiPassbackService;

    @BeforeEach
    void setUp() {
        offlinePinService = new OfflineSmartLockPinService(smartDeviceRepo, deviceOtpRepo);
        edgeSyncService = new BiometricEdgeSyncService(enrollmentRepo, deltaRepo);
        edgeSyncService.setMessagingTemplate(messagingTemplate);
        antiPassbackService = new AntiPassbackService(apbRepo);
    }

    @Test
    @DisplayName("Offline HKDF smart-lock PIN validates within 2h slot, expires outside window, and burns on single use")
    void shouldGenerateAndVerifyOfflineTimeBoundSmartLockPin() {
        SmartDevice lock = SmartDevice.builder()
                .id(42L).communityId(1L).deviceName("Flat A-402 Main Lock")
                .deviceType("DOOR_LOCK").location("A-402")
                .masterSecretHex("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")
                .build();

        when(smartDeviceRepo.findById(42L)).thenReturn(Optional.of(lock));
        when(deviceOtpRepo.save(any(DeviceVisitorOtp.class))).thenAnswer(inv -> {
            DeviceVisitorOtp o = inv.getArgument(0);
            o.setId(700L);
            return o;
        });

        // 2:00 PM to 4:00 PM aligned epoch
        Instant validFrom = Instant.ofEpochSecond(1_764_252_000L); // evenly divisible by 900s
        Instant validUntil = validFrom.plusSeconds(7200);

        var pinDesc = offlinePinService.generateTimeBoundPin(42L, 99L, "Plumber", validFrom, validUntil, true);
        assertThat(pinDesc.pinCode()).matches("\\d{6}");
        assertThat(pinDesc.derivationMode()).isEqualTo("HKDF_TIME_WINDOW");
        assertThat(pinDesc.slotDurationBuckets()).isEqualTo(8);

        Set<String> spentFirmwareDigests = new HashSet<>();

        // 2:30 PM -> Valid first entry
        var firstEntry = offlinePinService.verifyOnOfflineLockFirmware(
                lock.getMasterSecretHex(), 42L, pinDesc.pinCode(), validFrom.plusSeconds(1800), spentFirmwareDigests);
        assertThat(firstEntry.valid()).isTrue();
        assertThat(firstEntry.reason()).isEqualTo("AUTHORIZED_SINGLE_USE");

        // 3:15 PM -> Burned single-use PIN blocked cleanly on offline firmware
        var reuseAttempt = offlinePinService.verifyOnOfflineLockFirmware(
                lock.getMasterSecretHex(), 42L, pinDesc.pinCode(), validFrom.plusSeconds(4500), spentFirmwareDigests);
        assertThat(reuseAttempt.valid()).isFalse();
        assertThat(reuseAttempt.reason()).isEqualTo("SINGLE_USE_PIN_ALREADY_BURNED");

        // 4:10 PM -> Expired outside window (clean fresh firmware set)
        var expiredAttempt = offlinePinService.verifyOnOfflineLockFirmware(
                lock.getMasterSecretHex(), 42L, pinDesc.pinCode(), validUntil.plusSeconds(600), new HashSet<>());
        assertThat(expiredAttempt.valid()).isFalse();
        assertThat(expiredAttempt.reason()).isEqualTo("PIN_EXPIRED_OR_INVALID");
    }

    @Test
    @DisplayName("Biometric edge-sync pushes monotonic enrollment delta and urgent 2s blacklist kill-switch")
    @SuppressWarnings("unchecked")
    void shouldBroadcastBiometricDeltasAndPriorityKillSwitch() {
        Community comm = Community.builder().id(5L).name("Emerald Greens").build();
        AppUser maid = new AppUser();
        maid.setId(305L);
        BiometricUserEnrollment enrollment = BiometricUserEnrollment.builder()
                .id(12L).community(comm).user(maid)
                .personType(BiometricEnums.BiometricPersonType.DOMESTIC_STAFF)
                .personName("Lakshmi").faceEmbeddingHash("hash-lakshmi-512")
                .build();

        when(deltaRepo.findMaxSyncVersionByCommunityId(5L)).thenReturn(10L, 11L);
        when(enrollmentRepo.save(any(BiometricUserEnrollment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deltaRepo.save(any(BiometricEdgeDelta.class))).thenAnswer(inv -> {
            BiometricEdgeDelta d = inv.getArgument(0);
            d.setId(d.getSyncVersion());
            return d;
        });
        when(enrollmentRepo.findByCommunityIdAndUserId(5L, 305L)).thenReturn(Optional.of(enrollment));

        BiometricEdgeDelta upsertDelta = edgeSyncService.publishEnrollmentDelta(enrollment, "base64vec==");
        assertThat(upsertDelta.getSyncVersion()).isEqualTo(11L);
        assertThat(upsertDelta.getOp()).isEqualTo("UPSERT");
        verify(messagingTemplate).convertAndSend(eq("/topic/community/5/biometric/deltas"), any(Object.class));

        Map<String, Object> killRes = edgeSyncService.revokeAndBroadcastKillSwitch(5L, 305L, "TERMINATED_BY_MC");
        assertThat(killRes.get("op")).isEqualTo("REVOKE");
        assertThat(killRes.get("syncVersion")).isEqualTo(12L);
        assertThat(killRes.get("urgentKillSwitch")).isEqualTo(true);
        assertThat(enrollment.getEnrollmentStatus()).isEqualTo(BiometricEnums.EnrollmentStatus.REVOKED);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/community/5/biometric/revocations"), captor.capture());
        assertThat(((Map<String, Object>) captor.getValue()).get("urgentKillSwitch")).isEqualTo(true);
    }

    @Test
    @DisplayName("Anti-passback blocks <60s window bounce, grants soft override on stale missed exit, and detects tailgating")
    void shouldEnforceAntiPassbackAndTailgating() {
        Map<String, TurnstileApbState> store = new HashMap<>();
        when(apbRepo.findByCommunityIdAndZoneIdAndCredentialId(5L, "CLUBHOUSE_GYM", "RFID-4412"))
                .thenAnswer(inv -> Optional.ofNullable(store.get("RFID-4412")));
        when(apbRepo.save(any(TurnstileApbState.class))).thenAnswer(inv -> {
            TurnstileApbState s = inv.getArgument(0);
            store.put(s.getCredentialId(), s);
            return s;
        });

        LocalDateTime t0 = LocalDateTime.of(2026, 11, 27, 10, 0, 0);

        // 1. Resident enters gym normally at 10:00:00
        var first = antiPassbackService.evaluateSwipe(5L, "CLUBHOUSE_GYM", "RFID-4412", "ENTRY", "TURNSTILE_1", 1, t0);
        assertThat(first.allowed()).isTrue();
        assertThat(first.currentState()).isEqualTo("INSIDE");

        // 2. Hands card through window to friend 12 seconds later -> hard blocked
        var bounce = antiPassbackService.evaluateSwipe(5L, "CLUBHOUSE_GYM", "RFID-4412", "ENTRY", "TURNSTILE_1", 1, t0.plusSeconds(12));
        assertThat(bounce.allowed()).isFalse();
        assertThat(bounce.decision()).isEqualTo("DENIED_PASSBACK_COOLDOWN");
        assertThat(bounce.captureSecuritySnapshot()).isTrue();

        // 3. Stale missed exit (3 hours later) -> Soft APB recovery avoids locking out legitimate resident
        var softRecover = antiPassbackService.evaluateSwipe(5L, "CLUBHOUSE_GYM", "RFID-4412", "ENTRY", "TURNSTILE_2", 1, t0.plusHours(3));
        assertThat(softRecover.allowed()).isTrue();
        assertThat(softRecover.decision()).isEqualTo("GRANTED_SOFT_OVERRIDE");
        assertThat(softRecover.softOverrideApplied()).isTrue();

        // 4. Exit + re-entry with 2 torsos detected on IR optical beam -> Tailgating alert
        antiPassbackService.evaluateSwipe(5L, "CLUBHOUSE_GYM", "RFID-4412", "EXIT", "TURNSTILE_1", 1, t0.plusHours(4));
        var tailgateEntry = antiPassbackService.evaluateSwipe(5L, "CLUBHOUSE_GYM", "RFID-4412", "ENTRY", "TURNSTILE_1", 2, t0.plusHours(5));
        assertThat(tailgateEntry.allowed()).isTrue();
        assertThat(tailgateEntry.decision()).isEqualTo("GRANTED_WITH_TAILGATE_ALERT");
        assertThat(tailgateEntry.tailgateDetected()).isTrue();
        assertThat(tailgateEntry.captureSecuritySnapshot()).isTrue();
    }
}
