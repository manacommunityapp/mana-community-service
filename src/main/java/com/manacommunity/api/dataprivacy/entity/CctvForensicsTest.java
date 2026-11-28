package com.manacommunity.api.dataprivacy.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cctv_forensics_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CctvForensicsTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private ForensicScenario scenario;

    private String cameraId;
    private String cameraLocation;
    private int retentionDays;
    private boolean footageEncrypted;
    private boolean accessLogged;
    private String accessRequestedBy;
    private String accessApprovedBy;
    private boolean watermarked;
    private boolean tamperDetected;
    private boolean chainOfCustodyMaintained;
    private String exportFormat;
    private boolean hashVerified;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum ForensicScenario {
        FOOTAGE_ENCRYPTION_AT_REST, ACCESS_LOG_AUDIT_COMPLETE,
        UNAUTHORIZED_DOWNLOAD_BLOCKED, WATERMARK_ON_EXPORT,
        TAMPER_DETECTION_HASH_MISMATCH, CHAIN_OF_CUSTODY_BREAK,
        AUTO_DELETION_AFTER_RETENTION, POLICE_REQUEST_COMPLIANCE
    }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
