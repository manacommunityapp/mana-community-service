package com.manacommunity.api.audit;

import com.manacommunity.api.model.AuditLog;
import com.manacommunity.api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditIntegrityVerifier {

    private static final String DEFAULT_HMAC_SECRET = "mana-audit-tamper-resistant-immutable-secret-key-2026";
    private static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public AuditVerificationResult verifyChain(String tenantId) {
        List<AuditLog> records = auditLogRepository.findByTenantIdOrderBySequenceNumberAsc(tenantId);
        LocalDateTime verifiedAt = LocalDateTime.now();

        if (records.isEmpty()) {
            return AuditVerificationResult.builder()
                    .valid(true)
                    .tenantId(tenantId)
                    .totalRecordsVerified(0)
                    .verifiedAt(verifiedAt)
                    .build();
        }

        List<String> anomalies = new ArrayList<>();
        String expectedPrevHash = GENESIS_PREV_HASH;
        long expectedSeq = 1L;

        for (AuditLog logEntry : records) {
            Long currentSeq = logEntry.getSequenceNumber();
            if (currentSeq == null || currentSeq != expectedSeq) {
                anomalies.add("Sequence broken at ID=" + logEntry.getId() + ": Expected seq=" + expectedSeq + " but found " + currentSeq);
                return AuditVerificationResult.builder()
                        .valid(false)
                        .tenantId(tenantId)
                        .totalRecordsVerified(expectedSeq - 1)
                        .tamperedRecordId(logEntry.getId())
                        .tamperedSequenceNumber(currentSeq)
                        .failureReason("SEQUENCE_GAP_OR_OUT_OF_ORDER")
                        .anomalyDetails(anomalies)
                        .verifiedAt(verifiedAt)
                        .build();
            }

            if (!expectedPrevHash.equals(logEntry.getPrevHash())) {
                anomalies.add("PrevHash mismatch at Seq=" + currentSeq + ": Expected " + expectedPrevHash + " but found " + logEntry.getPrevHash());
                return AuditVerificationResult.builder()
                        .valid(false)
                        .tenantId(tenantId)
                        .totalRecordsVerified(expectedSeq - 1)
                        .tamperedRecordId(logEntry.getId())
                        .tamperedSequenceNumber(currentSeq)
                        .failureReason("PREV_HASH_CHAIN_BROKEN")
                        .anomalyDetails(anomalies)
                        .verifiedAt(verifiedAt)
                        .build();
            }

            String canonicalPayload = TamperResistantAuditService.computeCanonicalPayload(
                    logEntry.getTenantId(), logEntry.getSequenceNumber(), logEntry.getCreatedAt(),
                    logEntry.getUserId(), logEntry.getUsername(), logEntry.getAction(), logEntry.getModule(),
                    logEntry.getEntityName(), logEntry.getEntityId(), logEntry.getOldValue(), logEntry.getNewValue(),
                    logEntry.getCorrelationId(), logEntry.getIpAddress()
            );

            String expectedRecordHash = TamperResistantAuditService.hmacSha256Hex(DEFAULT_HMAC_SECRET, expectedPrevHash + "::" + canonicalPayload);
            if (!expectedRecordHash.equals(logEntry.getRecordHash())) {
                anomalies.add("Tampered row content at Seq=" + currentSeq + ": Content altered in database!");
                return AuditVerificationResult.builder()
                        .valid(false)
                        .tenantId(tenantId)
                        .totalRecordsVerified(expectedSeq - 1)
                        .tamperedRecordId(logEntry.getId())
                        .tamperedSequenceNumber(currentSeq)
                        .failureReason("PAYLOAD_TAMPERED_HASH_MISMATCH")
                        .anomalyDetails(anomalies)
                        .verifiedAt(verifiedAt)
                        .build();
            }

            expectedPrevHash = logEntry.getRecordHash();
            expectedSeq++;
        }

        return AuditVerificationResult.builder()
                .valid(true)
                .tenantId(tenantId)
                .totalRecordsVerified(records.size())
                .startSequenceNumber(records.get(0).getSequenceNumber())
                .endSequenceNumber(records.get(records.size() - 1).getSequenceNumber())
                .verifiedAt(verifiedAt)
                .build();
    }
}
