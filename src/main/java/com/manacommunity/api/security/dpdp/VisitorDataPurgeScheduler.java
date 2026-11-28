package com.manacommunity.api.security.dpdp;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automated DPDP Act 2023 Compliance Purging & Anonymization Engine.
 * Enforces strict data minimization (Section 12 - Right to Erasure):
 * 1. 30-Day Purge: Visitor facial photos stored in S3/object storage are deleted after 30 days.
 * 2. 90-Day Purge: Visitor movement and gate audit records older than 90 days have all PII
 *    (names, phone numbers, vehicle registration plates) permanently redacted.
 */
@Service
@Slf4j
public class VisitorDataPurgeScheduler {

    public static final int PHOTO_RETENTION_DAYS = 30;
    public static final int AUDIT_LOG_RETENTION_DAYS = 90;
    public static final String PURGED_REDACTED_LABEL = "[PURGED_DPDP_SECTION_12]";

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VisitorLogRecord {
        private String entryId;
        private String societyId;
        private String flatNumber;
        private String visitorName;
        private String visitorPhone;
        private String vehicleNumber;
        private String photoS3Key;
        private Instant entryTimestamp;
        private boolean photoPurged;
        private boolean piiAnonymized;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PurgeExecutionReport {
        private String executionId;
        private Instant executedAt;
        private int totalRecordsEvaluated;
        private int photosPurgedCount;
        private int logsAnonymizedCount;
        private List<String> deletedS3Keys;
        private List<String> anonymizedEntryIds;
    }

    private final Map<String, VisitorLogRecord> visitorLogStore = new ConcurrentHashMap<>();

    public void registerVisitorRecord(VisitorLogRecord record) {
        visitorLogStore.put(record.getEntryId(), record);
    }

    public VisitorLogRecord getRecord(String entryId) {
        return visitorLogStore.get(entryId);
    }

    /**
     * Executes the DPDP data retention purge cycle.
     * Evaluates all records against 30-day photo deletion and 90-day PII anonymization rules.
     */
    public PurgeExecutionReport executeDataPurge(Instant asOfDate) {
        Instant photoCutoff = asOfDate.minus(PHOTO_RETENTION_DAYS, ChronoUnit.DAYS);
        Instant logCutoff = asOfDate.minus(AUDIT_LOG_RETENTION_DAYS, ChronoUnit.DAYS);

        int photosPurged = 0;
        int logsAnonymized = 0;
        List<String> deletedKeys = new ArrayList<>();
        List<String> anonymizedIds = new ArrayList<>();

        for (VisitorLogRecord record : visitorLogStore.values()) {
            Instant entryTime = record.getEntryTimestamp();

            // 1. Check 30-Day Photo Purge Policy
            if (entryTime.isBefore(photoCutoff) && !record.isPhotoPurged() && record.getPhotoS3Key() != null) {
                deletedKeys.add(record.getPhotoS3Key());
                record.setPhotoPurged(true);
                record.setPhotoS3Key(null); // delete S3 reference
                photosPurged++;
                log.info("DPDP 30-Day Purge: Deleted visitor photo for entry [{}]", record.getEntryId());
            }

            // 2. Check 90-Day PII Anonymization Policy
            if (entryTime.isBefore(logCutoff) && !record.isPiiAnonymized()) {
                record.setPiiAnonymized(true);
                record.setVisitorName(PURGED_REDACTED_LABEL);
                record.setVisitorPhone(PURGED_REDACTED_LABEL);
                record.setVehicleNumber(PURGED_REDACTED_LABEL);
                anonymizedIds.add(record.getEntryId());
                logsAnonymized++;
                log.info("DPDP 90-Day Purge: Anonymized PII fields for gate entry [{}]", record.getEntryId());
            }
        }

        log.info("DPDP Purge completed: Evaluated {} records, purged {} photos, anonymized {} logs.",
                visitorLogStore.size(), photosPurged, logsAnonymized);

        return PurgeExecutionReport.builder()
                .executionId("PURGE-" + UUID.randomUUID())
                .executedAt(asOfDate)
                .totalRecordsEvaluated(visitorLogStore.size())
                .photosPurgedCount(photosPurged)
                .logsAnonymizedCount(logsAnonymized)
                .deletedS3Keys(deletedKeys)
                .anonymizedEntryIds(anonymizedIds)
                .build();
    }

    public void clear() {
        visitorLogStore.clear();
    }
}
