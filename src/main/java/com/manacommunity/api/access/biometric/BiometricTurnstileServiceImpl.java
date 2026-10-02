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
    public void setTurnstileStatus(Long turnstileId, TurnstileStatus status) {
        BiometricTurnstile turnstile = turnstileRepository.findById(turnstileId)
                .orElseThrow(() -> new ResourceNotFoundException("BiometricTurnstile", turnstileId));
        turnstile.setStatus(status);
        turnstileRepository.save(turnstile);
    }
}
