package com.manacommunity.api.unit.service;

import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.access.biometric.BiometricTurnstile;
import com.manacommunity.api.access.biometric.BiometricUserEnrollment;
import com.manacommunity.api.access.biometric.engine.TurnstileAccessRuleEngine;
import com.manacommunity.api.devices.entity.DeviceVisitorOtp;
import com.manacommunity.api.devices.entity.SmartDevice;
import com.manacommunity.api.devices.repository.DeviceVisitorOtpRepository;
import com.manacommunity.api.devices.repository.SmartDeviceRepository;
import com.manacommunity.api.devices.service.OfflineSmartLockPinService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.parking.engine.ParkingAllocationEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SmartLockAndAccessEdgeCasesTest {

    @Test
    void enforcesOfflinePinStartExpiryBoundariesAndClockDriftTolerance() {
        SmartDeviceRepository devRepo = Mockito.mock(SmartDeviceRepository.class);
        DeviceVisitorOtpRepository otpRepo = Mockito.mock(DeviceVisitorOtpRepository.class);
        OfflineSmartLockPinService pinService = new OfflineSmartLockPinService(devRepo, otpRepo);

        SmartDevice lock = SmartDevice.builder()
                .id(77L).communityId(1L).deviceName("B-402 Lock").deviceType("DOOR_LOCK")
                .masterSecretHex("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff")
                .build();
        when(devRepo.findById(77L)).thenReturn(Optional.of(lock));
        when(otpRepo.save(any(DeviceVisitorOtp.class))).thenAnswer(inv -> {
            DeviceVisitorOtp o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        Instant validFrom = Instant.ofEpochSecond(1_764_252_000L);
        Instant validUntil = validFrom.plusSeconds(3600); // 1 hour (4 buckets)
        var desc = pinService.generateTimeBoundPin(77L, 10L, "Delivery", validFrom, validUntil, false);

        // Lock RTC running 90s slow (shows validFrom - 90s) -> accepted within ±120s drift tolerance
        var withinDrift = pinService.verifyOnOfflineLockFirmwareWithDrift(
                lock.getMasterSecretHex(), 77L, desc.pinCode(), validFrom.minusSeconds(90), 120L, new HashSet<>());
        assertThat(withinDrift.valid()).isTrue();

        // Lock RTC skewed by 240s beyond ±120s tolerance -> rejected
        var outsideDrift = pinService.verifyOnOfflineLockFirmwareWithDrift(
                lock.getMasterSecretHex(), 77L, desc.pinCode(), validFrom.minusSeconds(240), 120L, new HashSet<>());
        assertThat(outsideDrift.valid()).isFalse();
        assertThat(outsideDrift.reason()).isEqualTo("PIN_EXPIRED_OR_INVALID");
    }

    @Test
    void blocksPhotoScreenSpoofAttemptsAndEnforcesVisitorBasementCapacity() {
        TurnstileAccessRuleEngine ruleEngine = new TurnstileAccessRuleEngine();
        Community comm = Community.builder().id(1L).name("Emerald").build();
        BiometricTurnstile turnstile = BiometricTurnstile.builder()
                .id(1L).turnstileIdentifier("G1-FACE").status(TurnstileStatus.ONLINE).community(comm).build();
        BiometricUserEnrollment staff = BiometricUserEnrollment.builder()
                .id(10L).personName("Kavita").personType(BiometricPersonType.DOMESTIC_STAFF)
                .enrollmentStatus(EnrollmentStatus.ENROLLED)
                .timeWindowStart("06:00").timeWindowEnd("22:00").allowedDays("MON,TUE,WED,THU,FRI,SAT,SUN")
                .build();

        LocalDateTime noon = LocalDateTime.of(2026, 11, 27, 12, 0);

        // High face confidence (0.98) but spoof liveness fails (0.42 < 0.75 printed photo) -> DENIED
        var spoofAttempt = ruleEngine.evaluate(
                turnstile, Optional.of(staff), new BigDecimal("0.98"), new BigDecimal("0.42"), noon);
        assertThat(spoofAttempt.unlockRelay()).isFalse();
        assertThat(spoofAttempt.decision()).isEqualTo(AccessDecision.DENIED_UNENROLLED);
        assertThat(spoofAttempt.reason()).contains("liveness failed");

        // Poor lighting -> confidence 0.68 < 0.80 -> DENIED
        var lowLight = ruleEngine.evaluate(
                turnstile, Optional.of(staff), new BigDecimal("0.68"), new BigDecimal("0.91"), noon);
        assertThat(lowLight.unlockRelay()).isFalse();

        // Live person + good lighting -> GRANTED
        var genuine = ruleEngine.evaluate(
                turnstile, Optional.of(staff), new BigDecimal("0.94"), new BigDecimal("0.92"), noon);
        assertThat(genuine.unlockRelay()).isTrue();
        assertThat(genuine.decision()).isEqualTo(AccessDecision.GRANTED_OPEN);

        // Visitor basement capacity limit (all Basement 1 visitor slots occupied -> null slot)
        ParkingAllocationEngine parkingEngine = new ParkingAllocationEngine();
        var fullBasement = List.of(
                new ParkingAllocationEngine.SlotCandidate(1L, "V-B1-01", "OPEN", "Visitor", "B1", true),
                new ParkingAllocationEngine.SlotCandidate(2L, "V-B1-02", "OPEN", "Visitor", "B1", true)
        );
        assertThat(parkingEngine.findBestSlot(fullBasement, "CAR", "Visitor")).isNull();
        assertThat(parkingEngine.calculateOccupancyRate(2, 2)).isEqualTo(100.0);
    }
}
