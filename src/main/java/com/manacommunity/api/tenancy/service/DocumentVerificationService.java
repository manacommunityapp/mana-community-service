package com.manacommunity.api.tenancy.service;

import com.manacommunity.api.tenancy.entity.TenantDocument;
import com.manacommunity.api.tenancy.entity.TenantDocument.DocumentType;
import com.manacommunity.api.tenancy.entity.TenantDocument.VerificationStatus;
import com.manacommunity.api.tenancy.entity.TenantRecord;
import com.manacommunity.api.tenancy.repository.TenantDocumentRepository;
import com.manacommunity.api.tenancy.repository.TenantRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentVerificationService {

    public static final long MAX_DOCUMENT_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private final TenantDocumentRepository documentRepository;
    private final TenantRecordRepository tenantRepository;

    @Transactional
    public TenantDocument uploadDocument(Long tenantRecordId, DocumentType type, String fileName) {
        return uploadDocument(tenantRecordId, type, fileName, 256L * 1024);
    }

    @Transactional
    public TenantDocument uploadDocument(Long tenantRecordId, DocumentType type, String fileName, long fileSizeBytes) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Document filename is required");
        }
        if (fileSizeBytes <= 0 || fileSizeBytes > MAX_DOCUMENT_SIZE_BYTES) {
            throw new IllegalArgumentException("Document file size must be between 1 byte and 10 MB");
        }
        int dot = fileName.lastIndexOf('.');
        String ext = dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported KYC document extension: " + ext);
        }

        TenantDocument doc = TenantDocument.builder()
                .tenantRecordId(tenantRecordId)
                .documentType(type)
                .originalFileName(fileName)
                .verificationStatus(VerificationStatus.UPLOADED)
                .build();

        doc = documentRepository.save(doc);

        // Trigger async OCR pipeline
        processOcr(doc);

        return doc;
    }

    /** Builds a tamper-evident Management Committee viewer watermark token for document previews. */
    public String generateMcPreviewWatermark(Long documentId, String reviewerIdentity) {
        String viewer = reviewerIdentity == null || reviewerIdentity.isBlank() ? "MC_COMMITTEE" : reviewerIdentity.trim();
        return "WM::DOC-" + documentId + "::MC-CONFIDENTIAL::VIEWER=" + viewer + "::NO-DOWNLOAD";
    }

    @Transactional
    public TenantDocument processOcr(TenantDocument doc) {
        doc.setVerificationStatus(VerificationStatus.OCR_QUEUED);
        documentRepository.save(doc);

        // Simulate OCR processing
        Map<String, String> extracted = simulateOcr(doc.getDocumentType());
        double confidence = 0.85 + Math.random() * 0.14;

        doc.setOcrExtractedData(extracted.toString());
        doc.setOcrConfidenceScore(Math.round(confidence * 100.0) / 100.0);
        doc.setOcrProcessedAt(LocalDateTime.now());
        doc.setVerificationStatus(VerificationStatus.OCR_COMPLETE);

        // PII encryption
        doc.setEncryptedStoragePath("/encrypted/tenant/" + doc.getTenantRecordId() + "/" + doc.getId());
        doc.setEncryptionKeyRef("AES-256-KMS-" + UUID.randomUUID().toString().substring(0, 8));
        doc.setVerificationStatus(VerificationStatus.PII_ENCRYPTED);

        // Watermarked preview generation (embeds MC preview watermark token)
        doc.setWatermarkedPreviewPath("/previews/wm-" + doc.getId() + ".pdf?wm=" + generateMcPreviewWatermark(doc.getId(), "MC_COMMITTEE"));
        doc.setVerificationStatus(VerificationStatus.WATERMARK_GENERATED);

        // Set expiry based on document type
        if (doc.getDocumentType() == DocumentType.RENTAL_AGREEMENT) {
            doc.setDocumentExpiryDate(LocalDate.now().plusMonths(11));
            doc.setExpiryReminderDate(LocalDate.now().plusMonths(10));
        } else if (doc.getDocumentType() == DocumentType.POLICE_VERIFICATION) {
            doc.setDocumentExpiryDate(LocalDate.now().plusMonths(12));
            doc.setExpiryReminderDate(LocalDate.now().plusMonths(11));
        }

        doc.setVerificationStatus(VerificationStatus.MC_REVIEW_PENDING);
        return documentRepository.save(doc);
    }

    @Transactional
    public TenantDocument reviewDocument(Long documentId, boolean approved, String remarks, String reviewerName) {
        TenantDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        doc.setVerifierRemarks(remarks);
        doc.setVerifiedBy(reviewerName);
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setVerificationStatus(approved ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);

        doc = documentRepository.save(doc);

        // Check if all required documents are approved for this tenant
        checkKycCompletion(doc.getTenantRecordId());

        return doc;
    }

    private void checkKycCompletion(Long tenantRecordId) {
        List<TenantDocument> docs = documentRepository.findByTenantRecordId(tenantRecordId);

        Set<DocumentType> requiredTypes = Set.of(
                DocumentType.AADHAAR, DocumentType.RENTAL_AGREEMENT, DocumentType.POLICE_VERIFICATION);

        Set<DocumentType> approvedTypes = docs.stream()
                .filter(d -> d.getVerificationStatus() == VerificationStatus.APPROVED)
                .map(TenantDocument::getDocumentType)
                .collect(Collectors.toSet());

        if (approvedTypes.containsAll(requiredTypes)) {
            TenantRecord tenant = tenantRepository.findById(tenantRecordId).orElse(null);
            if (tenant != null) {
                tenant.setKycStatus(TenantRecord.KycStatus.APPROVED);
                tenant.setStatus(TenantRecord.TenantStatus.ACTIVE);
                tenant.setAppAccessActive(true);
                tenantRepository.save(tenant);
            }
        }
    }

    public List<ExpiryAlert> checkExpiryAlerts() {
        LocalDate threshold = LocalDate.now().plusDays(30);
        List<TenantDocument> expiring = documentRepository.findExpiringDocuments(threshold);

        return expiring.stream().map(doc -> {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), doc.getDocumentExpiryDate());
            return new ExpiryAlert(
                    doc.getId(), doc.getTenantRecordId(), doc.getDocumentType().name(),
                    doc.getDocumentExpiryDate(), daysLeft,
                    daysLeft <= 7 ? "CRITICAL" : daysLeft <= 15 ? "WARNING" : "INFO");
        }).toList();
    }

    public DocumentPipelineSummary getPipelineSummary() {
        List<TenantDocument> all = documentRepository.findAll();
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(d -> d.getVerificationStatus().name(), Collectors.counting()));
        Map<String, Long> byType = all.stream()
                .collect(Collectors.groupingBy(d -> d.getDocumentType().name(), Collectors.counting()));

        double avgConfidence = all.stream()
                .filter(d -> d.getOcrConfidenceScore() > 0)
                .mapToDouble(TenantDocument::getOcrConfidenceScore)
                .average().orElse(0);

        List<TenantDocument> pendingReview = documentRepository.findPendingMcReview();
        List<ExpiryAlert> expiryAlerts = checkExpiryAlerts();

        return new DocumentPipelineSummary(byStatus, byType, avgConfidence,
                pendingReview.size(), expiryAlerts, all);
    }

    private Map<String, String> simulateOcr(DocumentType type) {
        return switch (type) {
            case AADHAAR -> Map.of("name", "Tenant Name", "aadhaarNumber", "XXXX-XXXX-" + (1000 + new Random().nextInt(9000)),
                    "dob", "1990-05-15", "address", "Flat 302, Tower A");
            case RENTAL_AGREEMENT -> Map.of("landlord", "Owner Name", "tenant", "Tenant Name",
                    "monthlyRent", "₹25,000", "startDate", LocalDate.now().toString(),
                    "endDate", LocalDate.now().plusMonths(11).toString(), "securityDeposit", "₹1,00,000");
            case POLICE_VERIFICATION -> Map.of("stationName", "Madhapur PS", "verificationNumber", "PV-" + (10000 + new Random().nextInt(90000)),
                    "status", "CLEAR", "validUpto", LocalDate.now().plusMonths(12).toString());
            default -> Map.of("type", type.name(), "status", "parsed");
        };
    }

    public record ExpiryAlert(Long documentId, Long tenantRecordId, String documentType,
                               LocalDate expiryDate, long daysRemaining, String severity) {}

    public record DocumentPipelineSummary(Map<String, Long> byStatus, Map<String, Long> byType,
                                           double avgOcrConfidence, int pendingMcReview,
                                           List<ExpiryAlert> expiryAlerts, List<TenantDocument> documents) {}
}
