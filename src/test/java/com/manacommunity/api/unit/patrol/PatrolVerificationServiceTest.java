package com.manacommunity.api.unit.patrol;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.patrol.dto.PatrolDtos.*;
import com.manacommunity.api.patrol.entity.PatrolCheckpointNode;
import com.manacommunity.api.patrol.entity.PatrolScanAudit;
import com.manacommunity.api.patrol.repository.PatrolRepositories.*;
import com.manacommunity.api.patrol.service.impl.PatrolVerificationServiceImpl;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatrolVerificationServiceTest {

    @Mock private PatrolCheckpointNodeRepository checkpointRepo;
    @Mock private PatrolScanAuditRepository scanAuditRepo;
    @Mock private PatrolBreachIncidentRepository breachRepo;

    @InjectMocks
    private PatrolVerificationServiceImpl patrolService;

    private Community community;
    private AppUser guard;
    private PatrolCheckpointNode checkpointGate1;

    @BeforeEach
    void setUp() {
        community = Community.builder().id(1L).name("Mana Palm Township").build();
        guard = AppUser.builder().id(99L).fullName("Guard Bahadur Singh").build();

        // Checkpoint 1 at Main Gate (lat: 12.9352, lon: 77.6245), geofence: 25m, NTAG tap counter: 100
        checkpointGate1 = PatrolCheckpointNode.builder()
                .id(10L)
                .community(community)
                .routeName("Night Perimeter Round")
                .sequenceOrder(1)
                .name("Main Gate Checkpoint")
                .latitude(new BigDecimal("12.9352000"))
                .longitude(new BigDecimal("77.6245000"))
                .geofenceRadiusMeters(25)
                .tagType("NTAG_424_DNA")
                .lastKnownTapCounter(100L)
                .allowedGraceMinutes(10)
                .build();
    }

    @Test
    @DisplayName("Rejects Android Mock Location GPS spoofing and records breach incident")
    void verifyAndRecordScan_mockGpsDetected_rejectsAndAlerts() {
        when(checkpointRepo.findById(10L)).thenReturn(Optional.of(checkpointGate1));
        when(scanAuditRepo.save(any(PatrolScanAudit.class))).thenAnswer(inv -> {
            PatrolScanAudit a = inv.getArgument(0);
            a.setId(501L);
            return a;
        });

        PatrolScanSubmission submission = PatrolScanSubmission.builder()
                .checkpointId(10L)
                .shiftId(1L)
                .scannedAtDeviceBootMs(123456789L)
                .latitude(new BigDecimal("12.9352000"))
                .longitude(new BigDecimal("77.6245000"))
                .gpsAccuracyMeters(new BigDecimal("5.0"))
                .isMockDetected(true) // Android Mock Provider hooked
                .nfcTapCounter(105L)
                .build();

        PatrolScanResult result = patrolService.verifyAndRecordScan(submission, guard);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.getValidityStatus()).isEqualTo("SPOOF_REJECTED");
        verify(breachRepo).save(any());
    }

    @Test
    @DisplayName("Rejects NTAG 424 DNA tap counter replay attack when counter does not monotonically advance")
    void verifyAndRecordScan_nfcReplay_rejectsReplayAttack() {
        when(checkpointRepo.findById(10L)).thenReturn(Optional.of(checkpointGate1));
        when(scanAuditRepo.save(any(PatrolScanAudit.class))).thenAnswer(inv -> {
            PatrolScanAudit a = inv.getArgument(0);
            a.setId(502L);
            return a;
        });

        // Current checkpoint counter is 100. Guard tries to resubmit counter 100 (replay)
        PatrolScanSubmission submission = PatrolScanSubmission.builder()
                .checkpointId(10L)
                .shiftId(1L)
                .scannedAtDeviceBootMs(123456789L)
                .latitude(new BigDecimal("12.9352000"))
                .longitude(new BigDecimal("77.6245000"))
                .gpsAccuracyMeters(new BigDecimal("5.0"))
                .isMockDetected(false)
                .nfcTapCounter(100L) // Stale counter!
                .build();

        PatrolScanResult result = patrolService.verifyAndRecordScan(submission, guard);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.getValidityStatus()).isEqualTo("REPLAY_REJECTED");
        verify(breachRepo).save(any());
    }

    @Test
    @DisplayName("Rejects scan when device location is outside checkpoint geofence radius")
    void verifyAndRecordScan_outsideGeofence_rejectsDistance() {
        when(checkpointRepo.findById(10L)).thenReturn(Optional.of(checkpointGate1));
        when(scanAuditRepo.save(any(PatrolScanAudit.class))).thenAnswer(inv -> {
            PatrolScanAudit a = inv.getArgument(0);
            a.setId(503L);
            return a;
        });

        // Scan at ~120 meters away from Main Gate
        PatrolScanSubmission submission = PatrolScanSubmission.builder()
                .checkpointId(10L)
                .shiftId(1L)
                .scannedAtDeviceBootMs(123456789L)
                .latitude(new BigDecimal("12.9362000"))
                .longitude(new BigDecimal("77.6255000"))
                .gpsAccuracyMeters(new BigDecimal("4.0"))
                .isMockDetected(false)
                .nfcTapCounter(105L)
                .build();

        PatrolScanResult result = patrolService.verifyAndRecordScan(submission, guard);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.getValidityStatus()).isEqualTo("OUT_OF_GEOFENCE");
    }

    @Test
    @DisplayName("Detects kinematic teleportation speed anomaly between successive checkpoints")
    void verifyAndRecordScan_teleportationSpeedAnomaly_flagsSpoof() {
        when(checkpointRepo.findById(10L)).thenReturn(Optional.of(checkpointGate1));

        // Previous scan was at Clubhouse (500m away) just 10 seconds ago (180 km/h impossible on foot)
        PatrolScanAudit previousScan = PatrolScanAudit.builder()
                .checkpoint(checkpointGate1)
                .guardUser(guard)
                .serverReceivedAt(LocalDateTime.now().minusSeconds(10))
                .latitude(new BigDecimal("12.9300000")) // ~600m away
                .longitude(new BigDecimal("77.6200000"))
                .scanValidityStatus("VALID")
                .build();
        when(scanAuditRepo.findFirstByGuardUser_IdOrderByServerReceivedAtDesc(99L))
                .thenReturn(Optional.of(previousScan));
        when(scanAuditRepo.save(any(PatrolScanAudit.class))).thenAnswer(inv -> {
            PatrolScanAudit a = inv.getArgument(0);
            a.setId(504L);
            return a;
        });

        PatrolScanSubmission submission = PatrolScanSubmission.builder()
                .checkpointId(10L)
                .shiftId(1L)
                .scannedAtDeviceBootMs(123456789L)
                .latitude(new BigDecimal("12.9352000"))
                .longitude(new BigDecimal("77.6245000"))
                .gpsAccuracyMeters(new BigDecimal("5.0"))
                .isMockDetected(false)
                .nfcTapCounter(105L)
                .build();

        PatrolScanResult result = patrolService.verifyAndRecordScan(submission, guard);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.getValidityStatus()).isEqualTo("KINEMATIC_TELEPORTATION_SPOOF");
        verify(breachRepo).save(any());
    }

    @Test
    @DisplayName("Approves valid scan inside geofence with monotonically advanced NTAG counter")
    void verifyAndRecordScan_validScan_approvesAndAdvancesCounter() {
        when(checkpointRepo.findById(10L)).thenReturn(Optional.of(checkpointGate1));
        when(scanAuditRepo.findFirstByGuardUser_IdOrderByServerReceivedAtDesc(99L))
                .thenReturn(Optional.empty());
        when(scanAuditRepo.save(any(PatrolScanAudit.class))).thenAnswer(inv -> {
            PatrolScanAudit a = inv.getArgument(0);
            a.setId(505L);
            return a;
        });

        PatrolScanSubmission submission = PatrolScanSubmission.builder()
                .checkpointId(10L)
                .shiftId(1L)
                .scannedAtDeviceBootMs(123456789L)
                .latitude(new BigDecimal("12.9352050")) // ~1m away
                .longitude(new BigDecimal("77.6245050"))
                .gpsAccuracyMeters(new BigDecimal("3.0"))
                .isMockDetected(false)
                .nfcTapCounter(101L) // Monotonically advanced beyond 100
                .build();

        PatrolScanResult result = patrolService.verifyAndRecordScan(submission, guard);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getValidityStatus()).isEqualTo("VALID");
        assertThat(checkpointGate1.getLastKnownTapCounter()).isEqualTo(101L);
        verify(checkpointRepo).save(checkpointGate1);
    }
}
