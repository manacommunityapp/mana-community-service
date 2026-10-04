package com.manacommunity.api.service;

import com.manacommunity.api.dto.FaceVerificationRequest;
import com.manacommunity.api.dto.FaceVerificationResult;
import com.manacommunity.api.dto.TurnstileOverrideRequest;
import com.manacommunity.api.model.AccessLogEntry;
import com.manacommunity.api.model.StaffScheduleRule;
import com.manacommunity.api.model.Turnstile;
import com.manacommunity.api.repository.AccessLogEntryRepository;
import com.manacommunity.api.repository.StaffScheduleRuleRepository;
import com.manacommunity.api.repository.TurnstileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BiometricAccessService {

    @Autowired
    private TurnstileRepository turnstileRepository;

    @Autowired
    private AccessLogEntryRepository accessLogRepository;

    @Autowired
    private StaffScheduleRuleRepository scheduleRuleRepository;

    public List<Turnstile> getTurnstiles(Long communityId) {
        return turnstileRepository.findBySocietyId(communityId);
    }

    @Transactional
    public FaceVerificationResult verifyFace(FaceVerificationRequest request) {
        Turnstile turnstile = turnstileRepository.findByTurnstileCode(request.getTurnstileCode())
                .orElse(null);

        if (turnstile == null) {
            return FaceVerificationResult.builder()
                    .decision("ACCESS_DENIED")
                    .reason("Unknown turnstile")
                    .turnstileCode(request.getTurnstileCode())
                    .build();
        }

        if ("LOCKED".equals(turnstile.getStatus()) || "MAINTENANCE".equals(turnstile.getStatus())) {
            String decision = "LOCKED".equals(turnstile.getStatus()) ? "LOCKDOWN_BLOCKED" : "ACCESS_DENIED";
            return FaceVerificationResult.builder()
                    .decision(decision)
                    .reason("Turnstile is " + turnstile.getStatus().toLowerCase())
                    .turnstileCode(request.getTurnstileCode())
                    .build();
        }

        Double confidence = request.getConfidenceScore() != null ? request.getConfidenceScore() : 0.0;
        boolean granted = confidence >= turnstile.getConfidenceThreshold();

        FaceVerificationResult result = FaceVerificationResult.builder()
                .decision(granted ? "ACCESS_GRANTED" : "ACCESS_DENIED")
                .confidenceScore(confidence)
                .reason(granted ? "Face verified" : "Confidence below threshold")
                .relayPulseDurationMs(granted ? turnstile.getUnlockDurationSeconds() * 1000 : null)
                .turnstileCode(request.getTurnstileCode())
                .build();

        AccessLogEntry log = AccessLogEntry.builder()
                .societyId(request.getSocietyId())
                .turnstileCode(request.getTurnstileCode())
                .turnstileName(turnstile.getTurnstileName())
                .decision(result.getDecision())
                .userId(result.getUserId())
                .userFullName(result.getUserFullName())
                .userType(result.getUserType())
                .confidenceScore(confidence)
                .reason(result.getReason())
                .timestamp(LocalDateTime.now())
                .build();
        accessLogRepository.save(log);

        return result;
    }

    public List<AccessLogEntry> getAccessLog(Long communityId, int page, int size) {
        return accessLogRepository.findBySocietyIdOrderByTimestampDesc(
                communityId, PageRequest.of(page, size));
    }

    public List<StaffScheduleRule> getStaffSchedules(Long communityId) {
        return scheduleRuleRepository.findBySocietyId(communityId);
    }

    @Transactional
    public StaffScheduleRule updateStaffSchedule(Long staffId, StaffScheduleRule rule) {
        return scheduleRuleRepository.findByStaffId(staffId).map(existing -> {
            existing.setAllowedStartTime(rule.getAllowedStartTime());
            existing.setAllowedEndTime(rule.getAllowedEndTime());
            existing.setAllowedDaysOfWeek(rule.getAllowedDaysOfWeek());
            existing.setIsActive(rule.getIsActive());
            return scheduleRuleRepository.save(existing);
        }).orElseGet(() -> {
            rule.setStaffId(staffId);
            return scheduleRuleRepository.save(rule);
        });
    }

    @Transactional
    public void overrideTurnstile(Long turnstileId, TurnstileOverrideRequest request) {
        turnstileRepository.findById(turnstileId).ifPresent(turnstile -> {
            switch (request.getAction()) {
                case "UNLOCK":
                    turnstile.setStatus("ONLINE");
                    break;
                case "LOCK":
                    turnstile.setStatus("LOCKED");
                    break;
                case "MAINTENANCE":
                    turnstile.setStatus("MAINTENANCE");
                    break;
            }
            turnstileRepository.save(turnstile);
        });
    }
}
