package com.manacommunity.api.tenancy.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantDocument extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantRecordId;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String originalFileName;
    private String encryptedStoragePath;
    private String watermarkedPreviewPath;
    private String encryptionKeyRef;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private String ocrExtractedData;
    private double ocrConfidenceScore;
    private LocalDateTime ocrProcessedAt;

    private String verifierRemarks;
    private String verifiedBy;
    private LocalDateTime verifiedAt;

    private LocalDate documentExpiryDate;
    private boolean expiryReminderSent;
    private LocalDate expiryReminderDate;

    public enum DocumentType {
        AADHAAR, RENTAL_AGREEMENT, POLICE_VERIFICATION, PAN_CARD,
        PASSPORT, OWNER_NOC, UTILITY_BILL
    }

    public enum VerificationStatus {
        UPLOADED, OCR_QUEUED, OCR_COMPLETE, PII_ENCRYPTED,
        WATERMARK_GENERATED, MC_REVIEW_PENDING, APPROVED, REJECTED, EXPIRED
    }
}
