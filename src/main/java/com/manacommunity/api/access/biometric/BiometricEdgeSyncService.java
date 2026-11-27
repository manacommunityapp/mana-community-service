package com.manacommunity.api.access.biometric;

import com.manacommunity.api.access.biometric.BiometricEnums.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dual-Lane Biometric Edge-Sync Coordinator across Township Gates 1..6:
 *  - Bulk Delta Lane (/topic/community/{cid}/biometric/deltas): propagates 512-d vectors within 5s SLA.
 *  - Priority Kill-Switch Lane (/topic/community/{cid}/biometric/revocations): <2s blacklist enforcement.
 */
@Service
@RequiredArgsConstructor
public class BiometricEdgeSyncService {

    private final BiometricUserEnrollmentRepository enrollmentRepo;
    private final BiometricEdgeDeltaRepository deltaRepo;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public BiometricEdgeDelta publishEnrollmentDelta(BiometricUserEnrollment enrollment, String vectorB64) {
        Long communityId = enrollment.getCommunity().getId();
        long nextVer = deltaRepo.findMaxSyncVersionByCommunityId(communityId) + 1;

        enrollment.setSyncVersion(nextVer);
        if (vectorB64 != null && !vectorB64.isBlank()) {
            enrollment.setEmbeddingVectorB64(vectorB64);
        }
        enrollmentRepo.save(enrollment);

        BiometricEdgeDelta delta = deltaRepo.save(BiometricEdgeDelta.builder()
                .communityId(communityId)
                .syncVersion(nextVer)
                .op("UPSERT")
                .enrollmentId(enrollment.getId())
                .personId(enrollment.getUser().getId())
                .personName(enrollment.getPersonName())
                .faceEmbeddingHash(enrollment.getFaceEmbeddingHash())
                .embeddingVectorB64(enrollment.getEmbeddingVectorB64())
                .reason("NEW_OR_UPDATED_ENROLLMENT")
                .build());

        Map<String, Object> payload = toDeltaPayload(delta, false);
        broadcast("/topic/community/" + communityId + "/biometric/deltas", payload);
        return delta;
    }

    @Transactional
    public Map<String, Object> revokeAndBroadcastKillSwitch(Long communityId, Long userId, String reason) {
        BiometricUserEnrollment enrollment = enrollmentRepo.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No biometric enrollment found for user " + userId + " in community " + communityId));

        long nextVer = deltaRepo.findMaxSyncVersionByCommunityId(communityId) + 1;
        enrollment.setEnrollmentStatus(EnrollmentStatus.REVOKED);
        enrollment.setRevokedAt(LocalDateTime.now());
        enrollment.setSyncVersion(nextVer);
        enrollmentRepo.save(enrollment);

        BiometricEdgeDelta delta = deltaRepo.save(BiometricEdgeDelta.builder()
                .communityId(communityId)
                .syncVersion(nextVer)
                .op("REVOKE")
                .enrollmentId(enrollment.getId())
                .personId(userId)
                .personName(enrollment.getPersonName())
                .faceEmbeddingHash(enrollment.getFaceEmbeddingHash())
                .reason(reason != null ? reason : "BLACKLISTED_KILL_SWITCH")
                .build());

        Map<String, Object> killPayload = toDeltaPayload(delta, true);
        // Push immediately to dedicated priority revocation topic (<2s SLA) AND bulk delta log for catch-up
        broadcast("/topic/community/" + communityId + "/biometric/revocations", killPayload);
        broadcast("/topic/community/" + communityId + "/biometric/deltas", killPayload);
        return killPayload;
    }

    @Transactional(readOnly = true)
    public List<BiometricEdgeDelta> getDeltasSince(Long communityId, long sinceVersion) {
        return deltaRepo.findByCommunityIdAndSyncVersionGreaterThanOrderBySyncVersionAsc(communityId, sinceVersion);
    }

    private Map<String, Object> toDeltaPayload(BiometricEdgeDelta delta, boolean urgentKillSwitch) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("communityId", delta.getCommunityId());
        map.put("syncVersion", delta.getSyncVersion());
        map.put("op", delta.getOp());
        map.put("personId", delta.getPersonId());
        map.put("personName", delta.getPersonName());
        map.put("faceEmbeddingHash", delta.getFaceEmbeddingHash());
        map.put("urgentKillSwitch", urgentKillSwitch);
        if (delta.getEmbeddingVectorB64() != null) {
            map.put("embeddingVectorB64", delta.getEmbeddingVectorB64());
        }
        if (delta.getReason() != null) {
            map.put("reason", delta.getReason());
        }
        return map;
    }

    private void broadcast(String topic, Map<String, Object> payload) {
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend(topic, (Object) payload);
            } catch (Exception ignored) {
                // Non-fatal in unit tests / offline broker outages
            }
        }
    }
}
