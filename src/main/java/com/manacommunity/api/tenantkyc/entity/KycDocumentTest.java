package com.manacommunity.api.tenantkyc.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kyc_document_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycDocumentTest extends BaseAuditEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityId;
    private String testCaseCode;
    private String testCaseName;

    @Enumerated(EnumType.STRING)
    private KycScenario scenario;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String fileName;
    private long fileSizeBytes;
    private long maxAllowedSizeBytes;
    private boolean fileSizeValid;

    private boolean ocrExtracted;
    private double ocrAccuracyPercent;
    private String ocrExtractedName;
    private String actualName;

    private boolean watermarkApplied;
    private String watermarkText;
    private boolean managementPreviewRestricted;

    private boolean leaseExpiryReminderSent;
    private String leaseExpiryDate;
    private int reminderDaysBefore;

    @Enumerated(EnumType.STRING)
    private TestResult testResult;
    private String failureReason;

    public enum KycScenario {
        AADHAAR_UPLOAD_OCR, RENTAL_AGREEMENT_UPLOAD, OVERSIZE_FILE_REJECTION,
        OCR_NAME_MISMATCH, WATERMARK_ON_MC_PREVIEW, LEASE_EXPIRY_11_MONTH_REMINDER,
        DOCUMENT_PRIVACY_NON_MC_ACCESS, CORRUPTED_PDF_UPLOAD
    }

    public enum DocumentType { AADHAAR, PAN, RENTAL_AGREEMENT, PASSPORT, UTILITY_BILL }

    public enum TestResult { PASS, FAIL, SKIPPED, PENDING }
}
