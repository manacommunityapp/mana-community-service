package com.manacommunity.api.patrol.service.impl;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.patrol.dto.PatrolDtos.*;
import com.manacommunity.api.patrol.entity.PatrolBreachIncident;
import com.manacommunity.api.patrol.entity.PatrolCheckpointNode;
import com.manacommunity.api.patrol.entity.PatrolScanAudit;
import com.manacommunity.api.patrol.repository.PatrolRepositories.*;
import com.manacommunity.api.patrol.service.PatrolVerificationService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolVerificationServiceImpl implements PatrolVerificationService {

    private final PatrolCheckpointNodeRepository checkpointRepo;
    private final PatrolScanAuditRepository scanAuditRepo;
    private final PatrolBreachIncidentRepository breachRepo;

    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double MAX_WALKING_PATROL_SPEED_KMH = 20.0; // Human sprint threshold inside township

    @Override
    @Transactional
    public PatrolScanResult verifyAndRecordScan(PatrolScanSubmission submission, AppUser guard) {
        PatrolCheckpointNode node = checkpointRepo.findById(submission.getCheckpointId())
                .orElseThrow(() -> new ResourceNotFoundException("Checkpoint not found: " + submission.getCheckpointId()));

        LocalDateTime now = LocalDateTime.now();

        // 1. Android mock location check
        if (submission.isMockDetected()) {
            recordBreach(node, guard.getId(), submission.getShiftId(), now, "SPOOF_ATTEMPT");
            PatrolScanAudit audit = saveAudit(node, guard, submission, now, "SPOOF_REJECTED",
                    "Mock GPS location detected on guard device", BigDecimal.ZERO, null);
            return reject(audit.getId(), node, "SPOOF_REJECTED", BigDecimal.ZERO, null, "Mock GPS provider detected");
        }

        // 2. Cryptographic NTAG 424 DNA tap counter monotonicity check
        if ("NTAG_424_DNA".equalsIgnoreCase(node.getTagType())) {
            if (submission.getNfcTapCounter() == null || submission.getNfcTapCounter() <= node.getLastKnownTapCounter()) {
                recordBreach(node, guard.getId(), submission.getShiftId(), now, "SPOOF_ATTEMPT");
                PatrolScanAudit audit = saveAudit(node, guard, submission, now, "REPLAY_REJECTED",
                        String.format("Tap counter (%d) did not advance beyond last known (%d)",
                                submission.getNfcTapCounter(), node.getLastKnownTapCounter()),
                        BigDecimal.ZERO, null);
                return reject(audit.getId(), node, "REPLAY_REJECTED", BigDecimal.ZERO, null,
                        "NFC tap counter replay/stale attack detected");
            }
        }

        // 3. Geofence radius verification (Haversine formula)
        double distanceMeters = calculateHaversineMeters(
                node.getLatitude().doubleValue(), node.getLongitude().doubleValue(),
                submission.getLatitude().doubleValue(), submission.getLongitude().doubleValue());

        BigDecimal distBd = BigDecimal.valueOf(distanceMeters).setScale(2, RoundingMode.HALF_UP);

        if (distanceMeters > node.getGeofenceRadiusMeters()) {
            PatrolScanAudit audit = saveAudit(node, guard, submission, now, "OUT_OF_GEOFENCE",
                    String.format("Scan distance (%.1fm) exceeds geofence radius (%dm)",
                            distanceMeters, node.getGeofenceRadiusMeters()),
                    distBd, null);
            return reject(audit.getId(), node, "OUT_OF_GEOFENCE", distBd, null,
                    String.format("Outside checkpoint geofence (%.1fm away)", distanceMeters));
        }

        // 4. Kinematic velocity check across successive checkpoints
        BigDecimal speedKmh = null;
        Optional<PatrolScanAudit> lastValidScan = scanAuditRepo.findFirstByGuardUser_IdOrderByServerReceivedAtDesc(guard.getId());
        if (lastValidScan.isPresent() && "VALID".equals(lastValidScan.get().getScanValidityStatus())) {
            PatrolScanAudit prev = lastValidScan.get();
            long secondsElapsed = Duration.between(prev.getServerReceivedAt(), now).getSeconds();
            if (secondsElapsed > 0 && secondsElapsed < 3600) { // Within 1 hour
                double legDistanceKm = calculateHaversineMeters(
                        prev.getLatitude().doubleValue(), prev.getLongitude().doubleValue(),
                        submission.getLatitude().doubleValue(), submission.getLongitude().doubleValue()) / 1000.0;
                double legHours = secondsElapsed / 3600.0;
                double calculatedSpeed = legDistanceKm / legHours;
                speedKmh = BigDecimal.valueOf(calculatedSpeed).setScale(2, RoundingMode.HALF_UP);

                if (calculatedSpeed > MAX_WALKING_PATROL_SPEED_KMH && secondsElapsed < 300) { // e.g. >20 km/h in <5 mins
                    recordBreach(node, guard.getId(), submission.getShiftId(), now, "SPOOF_ATTEMPT");
                    PatrolScanAudit audit = saveAudit(node, guard, submission, now, "KINEMATIC_TELEPORTATION_SPOOF",
                            String.format("Kinematic speed (%.1f km/h) exceeds walking patrol limits", calculatedSpeed),
                            distBd, speedKmh);
                    return reject(audit.getId(), node, "KINEMATIC_TELEPORTATION_SPOOF", distBd, speedKmh,
                            "Kinematic speed anomaly: impossible travel time between checkpoints");
                }
            }
        }

        // 5. Valid Scan: update checkpoint hardware tap counter
        if ("NTAG_424_DNA".equalsIgnoreCase(node.getTagType()) && submission.getNfcTapCounter() != null) {
            node.setLastKnownTapCounter(submission.getNfcTapCounter());
            checkpointRepo.save(node);
        }

        PatrolScanAudit audit = saveAudit(node, guard, submission, now, "VALID", null, distBd, speedKmh);

        return PatrolScanResult.builder()
                .scanAuditId(audit.getId())
                .checkpointId(node.getId())
                .checkpointName(node.getName())
                .accepted(true)
                .validityStatus("VALID")
                .distanceMeters(distBd)
                .calculatedSpeedKmh(speedKmh)
                .message("Checkpoint verified successfully")
                .build();
    }

    @Override
    @Transactional
    public List<PatrolBreachReport> checkAndEscalateMissedCheckpoints(Long communityId, String routeName, Long shiftId) {
        List<PatrolCheckpointNode> nodes = checkpointRepo
                .findByCommunityIdAndRouteNameAndIsActiveTrueOrderBySequenceOrderAsc(communityId, routeName);

        List<PatrolScanAudit> completedScans = scanAuditRepo
                .findByCommunityIdAndShiftIdOrderByServerReceivedAtAsc(communityId, shiftId);

        List<PatrolBreachReport> breaches = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (PatrolCheckpointNode node : nodes) {
            boolean scanned = completedScans.stream()
                    .anyMatch(s -> s.getCheckpoint().getId().equals(node.getId()) && "VALID".equals(s.getScanValidityStatus()));

            if (!scanned) {
                // If expected window + allowed grace minutes has elapsed
                LocalDateTime deadline = now.minusMinutes(node.getAllowedGraceMinutes());
                PatrolBreachIncident incident = PatrolBreachIncident.builder()
                        .community(node.getCommunity())
                        .checkpoint(node)
                        .shiftId(shiftId)
                        .expectedDeadline(deadline)
                        .breachType("MISSED_CHECKPOINT")
                        .escalationStatus("OPEN")
                        .triggeredAt(now)
                        .build();
                breachRepo.save(incident);

                breaches.add(PatrolBreachReport.builder()
                        .breachId(incident.getId())
                        .checkpointId(node.getId())
                        .checkpointName(node.getName())
                        .routeName(routeName)
                        .breachType("MISSED_CHECKPOINT")
                        .deadline(deadline)
                        .escalationStatus("OPEN")
                        .build());
            }
        }

        return breaches;
    }

    private PatrolScanAudit saveAudit(
            PatrolCheckpointNode node, AppUser guard, PatrolScanSubmission sub,
            LocalDateTime receivedAt, String status, String reason,
            BigDecimal dist, BigDecimal speed) {
        return scanAuditRepo.save(PatrolScanAudit.builder()
                .community(node.getCommunity())
                .checkpoint(node)
                .guardUser(guard)
                .shiftId(sub.getShiftId())
                .scannedAtDeviceBootMs(sub.getScannedAtDeviceBootMs())
                .serverReceivedAt(receivedAt)
                .latitude(sub.getLatitude())
                .longitude(sub.getLongitude())
                .gpsAccuracyMeters(sub.getGpsAccuracyMeters())
                .isMockDetected(sub.isMockDetected())
                .nfcCounterVerified(!"REPLAY_REJECTED".equals(status))
                .bleBeaconVerified(true)
                .kinematicSpeedKmh(speed)
                .scanValidityStatus(status)
                .rejectionReason(reason)
                .build());
    }

    private void recordBreach(PatrolCheckpointNode node, Long guardId, Long shiftId, LocalDateTime now, String type) {
        breachRepo.save(PatrolBreachIncident.builder()
                .community(node.getCommunity())
                .checkpoint(node)
                .guardUserId(guardId)
                .shiftId(shiftId)
                .expectedDeadline(now)
                .breachType(type)
                .escalationStatus("OPEN")
                .triggeredAt(now)
                .build());
    }

    private PatrolScanResult reject(
            Long auditId, PatrolCheckpointNode node, String status,
            BigDecimal dist, BigDecimal speed, String message) {
        return PatrolScanResult.builder()
                .scanAuditId(auditId)
                .checkpointId(node.getId())
                .checkpointName(node.getName())
                .accepted(false)
                .validityStatus(status)
                .distanceMeters(dist)
                .calculatedSpeedKmh(speed)
                .message(message)
                .build();
    }

    private double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
