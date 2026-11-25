package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.BiometricEnums.*;
import com.manacommunity.api.access.biometric.dto.BiometricDtos.*;
import com.manacommunity.api.access.biometric.engine.TurnstileAccessRuleEngine;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricTurnstileServiceImpl implements BiometricTurnstileService {

    private final BiometricTurnstileRepository turnstileRepository;
    private final BiometricUserEnrollmentRepository enrollmentRepository;
    private final BiometricAccessLogRepository accessLogRepository;
    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final TurnstileAccessRuleEngine ruleEngine;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.manacommunity.api.hrm.service.TimeGuardEvaluatorService timeGuardEvaluatorService;

    @Override
    @Transactional
    public VerifyFaceResult verifyFace(VerifyFaceRequest request) {
        BiometricTurnstile turnstile = turnstileRepository.findByTurnstileIdentifier(request.getTurnstileIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Unknown turnstile: " + request.getTurnstileIdentifier()));

        turnstile.setLastHeartbeat(LocalDateTime.now());
        turnstileRepository.save(turnstile);

        Optional<BiometricUserEnrollment> enrollmentOpt = enrollmentRepository.findByFaceEmbeddingHash(request.getFaceEmbeddingHash());

        LocalDateTime now = LocalDateTime.now();
        var eval = ruleEngine.evaluate(turnstile, enrollmentOpt, request.getConfidenceScore(), now);

        String personName = enrollmentOpt.map(BiometricUserEnrollment::getPersonName).orElse("Unknown Person");
        BiometricPersonType personType = enrollmentOpt.map(BiometricUserEnrollment::getPersonType).orElse(BiometricPersonType.VENDOR_WORKER);
        String unitNumber = enrollmentOpt.map(BiometricUserEnrollment::getUnitNumber).orElse(null);
        Long userId = enrollmentOpt.map(e -> e.getUser().getId()).orElse(null);

        BiometricAccessLog accessLog = BiometricAccessLog.builder()
                .turnstile(turnstile)
                .community(turnstile.getCommunity())
                .userId(userId)
                .personType(personType)
                .personName(personName)
                .unitNumber(unitNumber)
                .confidenceScore(request.getConfidenceScore())
                .accessDecision(eval.decision())
                .failureReason(eval.reason())
                .snapshotUrl(request.getSnapshotUrl())
                .timestamp(now)
                .build();

        accessLogRepository.save(accessLog);

        // Real-time WebSocket broadcast to guard security desktop
        messagingTemplate.convertAndSend(
                "/topic/community/" + turnstile.getCommunity().getId() + "/turnstile",
                (Object) Map.of(
                        "turnstile", turnstile.getTurnstileIdentifier(),
                        "decision", eval.decision().name(),
                        "personName", personName,
                        "personType", personType.name(),
                        "confidence", request.getConfidenceScore(),
                        "relayUnlock", eval.unlockRelay(),
                        "timestamp", now.toString()
                )
        );

        // Hardware IoT Relay Pulse for physical turnstile controllers
        if (eval.unlockRelay()) {
            messagingTemplate.convertAndSend(
                    "/topic/access/turnstiles/" + turnstile.getTurnstileIdentifier() + "/relay",
                    (Object) Map.of(
                            "turnstileIdentifier", turnstile.getTurnstileIdentifier(),
                            "action", "PULSE_UNLOCK",
                            "durationMs", turnstile.getRelayUnlockMs() != null ? turnstile.getRelayUnlockMs() : 3000,
                            "personName", personName,
                            "timestamp", now.toString()
                    )
            );
        }

        return VerifyFaceResult.builder()
                .turnstileIdentifier(turnstile.getTurnstileIdentifier())
                .decision(eval.decision())
                .relayUnlock(eval.unlockRelay())
                .relayUnlockDurationMs(turnstile.getRelayUnlockMs())
                .personName(personName)
                .personType(personType)
                .unitNumber(unitNumber)
                .confidenceScore(request.getConfidenceScore())
                .message(eval.reason())
                .build();
    }

    @Override
    @Transactional
    public void enrollUser(EnrollBiometricRequest request) {
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new ResourceNotFoundException("Community", request.getCommunityId()));
        AppUser user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", request.getUserId()));

        BiometricUserEnrollment enrollment = enrollmentRepository.findByUserId(user.getId())
                .orElseGet(() -> BiometricUserEnrollment.builder()
                        .community(community)
                        .user(user)
                        .build());

        enrollment.setPersonType(request.getPersonType());
        enrollment.setPersonName(request.getPersonName());
        enrollment.setUnitNumber(request.getUnitNumber());
        enrollment.setFaceEmbeddingHash(request.getFaceEmbeddingHash());
        enrollment.setEnrollmentStatus(EnrollmentStatus.ENROLLED);
        if (request.getTimeWindowStart() != null) enrollment.setTimeWindowStart(request.getTimeWindowStart());
        if (request.getTimeWindowEnd() != null) enrollment.setTimeWindowEnd(request.getTimeWindowEnd());
        if (request.getAllowedDays() != null) enrollment.setAllowedDays(request.getAllowedDays());

        enrollmentRepository.save(enrollment);
        log.info("Biometric face enrollment registered for user {} ({})", user.getId(), request.getPersonName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiometricTurnstileDto> getTurnstilesForCommunity(Long communityId) {
        return turnstileRepository.findByCommunityId(communityId).stream()
                .map(t -> BiometricTurnstileDto.builder()
                        .id(t.getId())
                        .turnstileIdentifier(t.getTurnstileIdentifier())
                        .turnstileName(t.getTurnstileName())
                        .communityId(t.getCommunity().getId())
                        .gateLocation(t.getGateLocation())
                        .direction(t.getDirection())
                        .status(t.getStatus())
                        .relayUnlockMs(t.getRelayUnlockMs())
                        .lastHeartbeat(t.getLastHeartbeat())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BiometricAccessLogDto> getAccessLogs(Long communityId, Pageable pageable) {
        return accessLogRepository.findByCommunityIdOrderByTimestampDesc(communityId, pageable)
                .map(l -> BiometricAccessLogDto.builder()
                        .id(l.getId())
                        .turnstileId(l.getTurnstile().getId())
                        .turnstileName(l.getTurnstile().getTurnstileName())
                        .personName(l.getPersonName())
                        .personType(l.getPersonType())
                        .unitNumber(l.getUnitNumber())
                        .confidenceScore(l.getConfidenceScore())
                        .accessDecision(l.getAccessDecision())
                        .failureReason(l.getFailureReason())
                        .snapshotUrl(l.getSnapshotUrl())
                        .timestamp(l.getTimestamp())
                        .build());
    }

    @Override
    @Transactional
    public void setTurnstileStatus(Long communityId, Long turnstileId, TurnstileStatus status) {
        BiometricTurnstile turnstile = turnstileRepository.findById(turnstileId)
                .orElseThrow(() -> new ResourceNotFoundException("BiometricTurnstile", turnstileId));
        if (turnstile.getCommunity() == null || !turnstile.getCommunity().getId().equals(communityId)) {
            throw new org.springframework.security.access.AccessDeniedException("Cross-community access denied");
        }
        turnstile.setStatus(status);
        turnstileRepository.save(turnstile);
    }

    @Override
    @Transactional
    public KioskAttendanceResult recordKioskAttendance(KioskAttendanceRequest request) {
        VerifyFaceResult verifyResult = verifyFace(VerifyFaceRequest.builder()
                .turnstileIdentifier(request.getTurnstileIdentifier())
                .faceEmbeddingHash(request.getFaceEmbeddingHash())
                .confidenceScore(request.getConfidenceScore())
                .snapshotUrl(request.getSnapshotUrl())
                .build());

        String shift = "Standard Day Shift (08:00 - 17:00)";
        String workHours = "CLOCK_OUT".equalsIgnoreCase(request.getPunchType()) ? "8 hrs 15 mins" : null;
        String agency = verifyResult.getPersonType() == BiometricPersonType.VENDOR_WORKER ? "Apex Facility Solutions Pvt Ltd" : null;

        // Asynchronously evaluate AI TimeGuard anomalies (geo-jumps, buddy punching, spoofing)
        timeGuardEvaluatorService.evaluatePunch(request, verifyResult, 1L, 1L);

        return KioskAttendanceResult.builder()
                .turnstileIdentifier(verifyResult.getTurnstileIdentifier())
                .decision(verifyResult.getDecision())
                .relayUnlock(verifyResult.isRelayUnlock())
                .relayUnlockDurationMs(verifyResult.getRelayUnlockDurationMs())
                .personName(verifyResult.getPersonName())
                .personType(verifyResult.getPersonType())
                .unitNumber(verifyResult.getUnitNumber())
                .confidenceScore(verifyResult.getConfidenceScore())
                .message(verifyResult.getMessage() + ("CLOCK_IN".equalsIgnoreCase(request.getPunchType()) ? " [Clock-In Recorded]" : " [Clock-Out Recorded]"))
                .shiftName(shift)
                .workHoursToday(workHours)
                .punchType(request.getPunchType())
                .contractorAgencyName(agency)
                .autoAssignedShift(true)
                .build();
    }

    @Override
    @Transactional
    public OfflinePunchSyncResponse syncOfflinePunches(OfflinePunchSyncRequest request) {
        if (request.getPunches() == null || request.getPunches().isEmpty()) {
            return OfflinePunchSyncResponse.builder().syncedCount(0).failedCount(0).message("No punches to sync").build();
        }

        int synced = 0;
        int failed = 0;
        for (OfflinePunchRecordDto punch : request.getPunches()) {
            try {
                verifyFace(VerifyFaceRequest.builder()
                        .turnstileIdentifier(punch.getTurnstileIdentifier())
                        .faceEmbeddingHash(punch.getFaceEmbeddingHash())
                        .confidenceScore(punch.getConfidenceScore() != null ? punch.getConfidenceScore() : java.math.BigDecimal.valueOf(0.95))
                        .snapshotUrl(punch.getSnapshotUrl())
                        .build());
                synced++;
            } catch (Exception e) {
                log.warn("Failed to replay offline punch {}: {}", punch.getId(), e.getMessage());
                failed++;
            }
        }

        return OfflinePunchSyncResponse.builder()
                .syncedCount(synced)
                .failedCount(failed)
                .message(String.format("Successfully synced %d offline punches, %d failed", synced, failed))
                .build();
    }
}
