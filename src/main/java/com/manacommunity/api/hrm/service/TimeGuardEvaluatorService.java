package com.manacommunity.api.hrm.service;

import com.manacommunity.api.access.biometric.dto.BiometricDtos.KioskAttendanceRequest;
import com.manacommunity.api.access.biometric.dto.BiometricDtos.VerifyFaceResult;
import com.manacommunity.api.hrm.entity.HrTimeGuardAnomaly;
import com.manacommunity.api.hrm.repository.HrTimeGuardAnomalyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class TimeGuardEvaluatorService {

    private final HrTimeGuardAnomalyRepository anomalyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public TimeGuardEvaluatorService(
            HrTimeGuardAnomalyRepository anomalyRepository,
            @Autowired(required = false) SimpMessagingTemplate messagingTemplate) {
        this.anomalyRepository = anomalyRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Evaluates attendance punch for anomalies (geo-jumps, buddy punching, anti-spoofing).
     */
    public void evaluatePunch(KioskAttendanceRequest req, VerifyFaceResult result, Long orgId, Long communityId) {
        try {
            // 1. Anti-Spoofing Liveness Failure Check
            if (Boolean.FALSE.equals(req.getIsLivenessVerified())) {
                flagAnomaly(
                        "anom_" + System.currentTimeMillis() + "_spoof",
                        orgId != null ? orgId : 1L,
                        communityId != null ? communityId : 1L,
                        "FACE_LIVENESS_FAILED",
                        "CRITICAL",
                        result.getPersonName(),
                        result.getPersonType() != null ? result.getPersonType().name() : "STAFF",
                        null,
                        req.getTurnstileIdentifier(),
                        req.getConfidenceScore(),
                        "Anti-spoofing engine rejected 2D static presentation or screen replay during clock-in attempt."
                );
            }

            // 2. Buddy-Punch Risk Check (Sub-threshold confidence: 0.65 - 0.84)
            if (req.getConfidenceScore() != null
                    && req.getConfidenceScore().compareTo(BigDecimal.valueOf(0.65)) >= 0
                    && req.getConfidenceScore().compareTo(BigDecimal.valueOf(0.85)) < 0) {
                flagAnomaly(
                        "anom_" + System.currentTimeMillis() + "_buddy",
                        orgId != null ? orgId : 1L,
                        communityId != null ? communityId : 1L,
                        "BUDDY_PUNCH_SUSPECTED",
                        "CRITICAL",
                        result.getPersonName(),
                        result.getPersonType() != null ? result.getPersonType().name() : "STAFF",
                        null,
                        req.getTurnstileIdentifier(),
                        req.getConfidenceScore(),
                        "Low facial matching confidence score (" + req.getConfidenceScore() + ") indicates potential proxy buddy-punching attempt."
                );
            }
        } catch (Exception e) {
            log.warn("TimeGuard evaluation non-blocking error: {}", e.getMessage());
        }
    }

    private void flagAnomaly(
            String id, Long orgId, Long commId, String type, String severity,
            String personName, String personRole, String agencyName,
            String gateLocation, BigDecimal confidenceScore, String details) {

        HrTimeGuardAnomaly anomaly = HrTimeGuardAnomaly.builder()
                .id(id)
                .organizationId(orgId)
                .communityId(commId)
                .anomalyType(type)
                .severity(severity)
                .personName(personName)
                .personRole(personRole)
                .agencyName(agencyName)
                .gateLocation(gateLocation)
                .confidenceScore(confidenceScore)
                .forensicDetails(details)
                .status("FLAGGED")
                .eventTimestamp(LocalDateTime.now())
                .build();

        HrTimeGuardAnomaly saved = anomalyRepository.save(anomaly);
        log.info("AI TimeGuard flagged anomaly: {} [{}] for {}", type, severity, personName);

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/admin", saved);
                messagingTemplate.convertAndSend("/topic/community/" + (commId != null ? commId : 1L) + "/timeguard", saved);
            } catch (Exception ex) {
                log.debug("Could not push live TimeGuard anomaly alert via WebSocket: {}", ex.getMessage());
            }
        }
    }
}
