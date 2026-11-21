package com.manacommunity.api.document.service;

import com.manacommunity.api.document.dto.*;
import com.manacommunity.api.document.entity.*;
import com.manacommunity.api.document.repository.*;
import com.manacommunity.api.media.config.MediaProperties;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepo;
    private final DocumentVersionRepository versionRepo;
    private final DocumentAccessGrantRepository grantRepo;
    private final DocumentDownloadLogRepository downloadLogRepo;
    private final DocumentS3Service s3Service;
    private final DocumentScanService scanService;
    private final DocumentAccessService accessService;
    private final MediaProperties mediaProperties;

    public DocumentDto upload(DocumentUploadRequest request, MultipartFile file, UserPrincipal principal) {
        validateFile(file);
        Document doc = new Document();
        doc.setCommunityId(principal.getCommunityId());
        doc.setTitle(request.title());
        doc.setDescription(request.description());
        doc.setCategory(request.category());
        doc.setEntityType(request.entityType());
        doc.setEntityId(request.entityId());
        doc.setOwnerUserId(principal.getUserId());
        doc.setStatus(DocumentStatus.PENDING_SCAN);
        doc.setAccessLevel(request.accessLevel() != null ? request.accessLevel() : DocAccessLevel.OWNER_ONLY);
        if (request.expiresAt() != null)
            doc.setExpiresAt(request.expiresAt().atStartOfDay(ZoneOffset.UTC).toInstant());
        doc.setReminderDaysBefore(request.reminderDaysBefore() != null ? request.reminderDaysBefore() : 30);
        doc.setRetentionDays(request.retentionDays());
        doc.setCreatedBy(principal.getUserId());
        doc = documentRepo.save(doc);

        DocumentVersion version = createVersion(doc, file, 1, principal.getUserId());
        doc.setStatus(DocumentStatus.ACTIVE);
        documentRepo.save(doc);

        scanService.scanAsync(version.getId());
        return toDto(doc, version);
    }

    public DocumentVersionDto addVersion(Long documentId, MultipartFile file, UserPrincipal principal) {
        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!accessService.canAccess(doc, principal.getUserId(), principalRole(principal), "MANAGE"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        validateFile(file);
        versionRepo.findByDocumentIdAndCurrentTrue(documentId)
                .ifPresent(v -> { v.setCurrent(false); versionRepo.save(v); });

        int nextVersion = versionRepo.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream().findFirst().map(v -> v.getVersionNumber() + 1).orElse(2);

        DocumentVersion version = createVersion(doc, file, nextVersion, principal.getUserId());
        scanService.scanAsync(version.getId());
        return toVersionDto(version);
    }

    public DocumentSignedUrlResponse generateSignedUrl(Long documentId, UserPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!accessService.canAccess(doc, principal.getUserId(), principalRole(principal), "DOWNLOAD"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        DocumentVersion current = versionRepo.findByDocumentIdAndCurrentTrue(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No current version"));
        if (DocScanStatus.INFECTED.equals(current.getScanStatus()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Document failed security scan");

        Duration expiry = Duration.ofMinutes(15);
        String url = s3Service.generatePresignedUrl(current.getS3Key(), current.getS3Bucket(), expiry);

        DocumentDownloadLog entry = new DocumentDownloadLog();
        entry.setDocumentId(documentId);
        entry.setVersionId(current.getId());
        entry.setDownloaderUserId(principal.getUserId());
        entry.setIpAddress(httpRequest.getRemoteAddr());
        entry.setDownloadedAt(Instant.now());
        downloadLogRepo.save(entry);

        return new DocumentSignedUrlResponse(url, Instant.now().plus(expiry));
    }

    public void recordScanResult(Long versionId, String result) {
        DocumentVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        version.setScanStatus("CLEAN".equalsIgnoreCase(result) ? DocScanStatus.CLEAN : DocScanStatus.INFECTED);
        version.setScanCompletedAt(Instant.now());
        versionRepo.save(version);
        if (DocScanStatus.INFECTED.equals(version.getScanStatus())) {
            documentRepo.findById(version.getDocumentId()).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.ARCHIVED);
                documentRepo.save(doc);
                log.warn("[DocScan] Document {} quarantined — infected file detected in version {}", doc.getId(), versionId);
            });
        }
    }

    public void softDelete(Long documentId, UserPrincipal principal) {
        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (doc.isLegalHold())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document is under legal hold and cannot be deleted");
        if (!accessService.canAccess(doc, principal.getUserId(), principalRole(principal), "MANAGE"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        doc.setStatus(DocumentStatus.DELETED);
        documentRepo.save(doc);
    }

    @Transactional(readOnly = true)
    public DocumentDto getDocument(Long id, UserPrincipal principal) {
        Document doc = documentRepo.findById(id)
                .filter(d -> !DocumentStatus.DELETED.equals(d.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!accessService.canAccess(doc, principal.getUserId(), principalRole(principal), "VIEW"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        DocumentVersion current = versionRepo.findByDocumentIdAndCurrentTrue(id).orElse(null);
        return toDto(doc, current);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionDto> getVersions(Long documentId, UserPrincipal principal) {
        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!accessService.canAccess(doc, principal.getUserId(), principalRole(principal), "VIEW"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return versionRepo.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream().map(this::toVersionDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listByEntity(String entityType, String entityId, UserPrincipal principal) {
        return documentRepo.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .filter(d -> !DocumentStatus.DELETED.equals(d.getStatus()))
                .filter(d -> accessService.canAccess(d, principal.getUserId(), principalRole(principal), "VIEW"))
                .map(d -> toDto(d, versionRepo.findByDocumentIdAndCurrentTrue(d.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listByCommunity(Long communityId, DocumentCategory category, UserPrincipal principal) {
        List<Document> docs = category != null
                ? documentRepo.findByCommunityIdAndCategory(communityId, category)
                : documentRepo.findByCommunityIdAndStatusNot(communityId, DocumentStatus.DELETED);
        return docs.stream()
                .filter(d -> !DocumentStatus.DELETED.equals(d.getStatus()))
                .filter(d -> accessService.canAccess(d, principal.getUserId(), principalRole(principal), "VIEW"))
                .map(d -> toDto(d, versionRepo.findByDocumentIdAndCurrentTrue(d.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDownloadLog> getAuditLog(Long documentId, UserPrincipal principal) {
        Document doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!accessService.canAccess(doc, principal.getUserId(), principalRole(principal), "MANAGE"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return downloadLogRepo.findByDocumentIdOrderByDownloadedAtDesc(documentId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private DocumentVersion createVersion(Document doc, MultipartFile file, int versionNumber, Long uploadedBy) {
        String bucket = mediaProperties.getS3().getBucket();
        String s3Key = s3Service.buildS3Key(doc.getCommunityId(), doc.getCategory(),
                doc.getId(), versionNumber, file.getOriginalFilename());
        try {
            s3Service.upload(s3Key, bucket, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }
        DocumentVersion v = new DocumentVersion();
        v.setDocumentId(doc.getId());
        v.setVersionNumber(versionNumber);
        v.setS3Key(s3Key);
        v.setS3Bucket(bucket);
        v.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        v.setContentType(file.getContentType());
        v.setFileSizeBytes(file.getSize());
        v.setScanStatus(DocScanStatus.PENDING);
        v.setCurrent(true);
        v.setUploadedBy(uploadedBy);
        v.setUploadedAt(Instant.now());
        return versionRepo.save(v);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        if (file.getSize() > 50L * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 50 MB limit");
    }

    private String principalRole(UserPrincipal principal) {
        if (principal.getUser() != null && principal.getUser().getRole() != null)
            return principal.getUser().getRole().toUpperCase();
        return principal.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
    }

    private DocumentDto toDto(Document doc, DocumentVersion version) {
        boolean expiryWarning = doc.getExpiresAt() != null &&
                doc.getExpiresAt().isBefore(Instant.now().plus(Duration.ofDays(doc.getReminderDaysBefore())));
        int versionCount = (int) versionRepo.countByDocumentId(doc.getId());
        return new DocumentDto(doc.getId(), doc.getTitle(), doc.getDescription(),
                doc.getCategory(), doc.getEntityType(), doc.getEntityId(),
                doc.getStatus(), doc.getAccessLevel(), doc.getExpiresAt(), expiryWarning,
                versionCount,
                version != null ? version.getOriginalFilename() : null,
                version != null ? version.getFileSizeBytes() : null,
                version != null ? version.getScanStatus() : null,
                doc.getCreatedAt(), doc.getCreatedBy());
    }

    private DocumentVersionDto toVersionDto(DocumentVersion v) {
        return new DocumentVersionDto(v.getId(), v.getVersionNumber(), v.getOriginalFilename(),
                v.getContentType(), v.getFileSizeBytes(), v.getScanStatus(),
                v.getUploadedAt(), v.getUploadedBy());
    }
}
