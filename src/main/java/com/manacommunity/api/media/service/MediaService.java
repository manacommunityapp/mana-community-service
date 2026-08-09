package com.manacommunity.api.media.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.media.config.MediaProperties;
import com.manacommunity.api.media.dto.*;
import com.manacommunity.api.media.entity.*;
import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.media.repository.MediaUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Core business logic for the Centralized Media Management Service.
 *
 * Responsibilities:
 *  - Validate and upload files to S3 (direct stream — no temp file on disk).
 *  - Generate presigned PUT URLs for large/video files (direct browser→S3 upload).
 *  - Save ONLY metadata to PostgreSQL. Files are NEVER stored in the DB.
 *  - Generate dynamic access URLs on demand. URLs are NEVER persisted.
 *  - Manage lifecycle: soft-delete, restore, approval workflow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository       mediaRepo;
    private final MediaUploadSessionRepository sessionRepo;
    private final S3MediaGateway        s3Gateway;
    private final MediaKeyGenerator     keyGen;
    private final MimeTypeDetector      mimeDetector;
    private final MediaUrlService       urlService;
    private final MediaProperties       props;
    private final MediaValidator        mediaValidator;

    // ── Direct multipart upload ──────────────────────────────────────────────

    /**
     * Upload a file directly through the server to S3.
     * Validates MIME type server-side, generates a UUID key, stores only metadata.
     *
     * @param file      multipart file from the HTTP request
     * @param request   metadata about the file's module/context
     * @param uploaderId  authenticated user ID
     */
    @Transactional
    public MediaResponse uploadMedia(MultipartFile file, MediaUploadRequest request, Long uploaderId) {
        // 1. Validate file is not empty
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // 2. Detect true MIME type from bytes (ignore client Content-Type)
        String mimeType  = mimeDetector.detect(file);
        String extension = mimeDetector.toExtension(mimeType);
        MediaType mediaType = mimeDetector.toMediaType(mimeType);

        // 3. Validate file size per media type
        validateSize(file.getSize(), mediaType);

        // 4. Deep format & corrupted file validation
        Integer width = null;
        Integer height = null;

        if (mediaType == MediaType.IMAGE) {
            MediaValidator.ImageDimensions dims = mediaValidator.validateAndExtractImageDimensions(file, mimeType);
            width = dims.width();
            height = dims.height();
        } else if (mediaType == MediaType.VIDEO) {
            mediaValidator.validateVideoFile(file, mimeType);
        }

        // 5. Generate UUID-based S3 key — never use original filename
        String s3Key = keyGen.generate(
                request.getModule(), request.getCommunityId(),
                request.getModuleId(), request.getSubContext(),
                mediaType, extension);

        String bucket = props.getS3().getBucket();

        // 6. Stream directly to S3 (no temp file created on server disk)
        try {
            s3Gateway.putObject(bucket, s3Key, file.getInputStream(), mimeType, file.getSize());
        } catch (Exception e) {
            log.error("S3 upload failed for key={}: {}", s3Key, e.getMessage(), e);
            throw new com.manacommunity.api.exception.MediaStorageException(
                    "Cloud storage upload failed (S3 bucket: " + bucket + "). Database record was NOT created. Details: " + e.getMessage(), e);
        }

        // 7. Persist ONLY metadata to PostgreSQL
        MediaObject media = MediaObject.builder()
                .module(request.getModule())
                .moduleId(request.getModuleId())
                .communityId(request.getCommunityId())
                .subContext(request.getSubContext())
                .originalFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file")
                .storedFileName(s3Key.substring(s3Key.lastIndexOf('/') + 1))
                .mimeType(mimeType)
                .extension(extension)
                .fileSize(file.getSize())
                .mediaType(mediaType)
                .bucketName(bucket)
                .s3Key(s3Key)
                .width(width)
                .height(height)
                .uploadedBy(uploaderId)
                .approvalRequired(request.isApprovalRequired())
                .status(request.isApprovalRequired() ? MediaStatus.PENDING : MediaStatus.ACTIVE)
                .caption(request.getCaption())
                .altText(request.getAltText())
                .featured(request.isFeatured())
                .sortOrder(request.getSortOrder())
                .build();

        media = mediaRepo.save(media);

        log.info("Media uploaded: externalId={} module={} moduleId={} key={}",
                media.getExternalId(), request.getModule(), request.getModuleId(), s3Key);

        return toResponse(media);
    }

    // ── Presigned PUT (direct browser → S3 for large files / videos) ─────────

    /**
     * Generate a presigned S3 PUT URL.
     * The client will PUT the file directly to S3, then call confirm.
     */
    @Transactional
    public PresignedUrlResponse generatePresignedUploadUrl(PresignedUploadRequest request, Long uploaderId) {
        mediaValidator.validatePresignedRequest(request);

        String bucket    = props.getS3().getBucket();
        int    expiryMin = props.getS3().getPresignedPutExpiryMinutes();

        // Derive extension from the client-supplied MIME type (hint only — confirmed on completion)
        String extension = mimeDetector.toExtension(request.getMimeType());

        // Generate the S3 key
        String s3Key = keyGen.generate(
                request.getModule(), request.getCommunityId(),
                request.getModuleId(), request.getSubContext(),
                request.getMediaType(), extension);

        // Generate presigned PUT URL
        PresignedPutObjectRequest presigned = s3Gateway.presignPut(
                bucket, s3Key, request.getMimeType(), Duration.ofMinutes(expiryMin));

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(expiryMin);

        // Save session — will be confirmed after client PUT
        MediaUploadSession session = MediaUploadSession.builder()
                .module(request.getModule())
                .moduleId(request.getModuleId())
                .communityId(request.getCommunityId())
                .subContext(request.getSubContext())
                .uploadedBy(uploaderId)
                .presignedKey(s3Key)
                .bucket(bucket)
                .mediaType(request.getMediaType())
                .originalName(request.getOriginalFileName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .expiresAt(expiresAt)
                .status("PENDING")
                .build();

        session = sessionRepo.save(session);

        log.info("Presigned PUT generated: sessionId={} module={} key={}",
                session.getSessionId(), request.getModule(), s3Key);

        return PresignedUrlResponse.builder()
                .sessionId(session.getSessionId())
                .presignedUploadUrl(presigned.url().toString())
                .httpMethod("PUT")
                .s3Key(s3Key)
                .contentType(request.getMimeType())
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Confirm that a presigned-PUT upload completed successfully.
     * Verifies the object exists in S3, then creates the MediaObject record.
     */
    @Transactional
    public MediaResponse confirmPresignedUpload(UUID sessionId, Long uploaderId) {
        MediaUploadSession session = sessionRepo.findBySessionIdAndStatus(sessionId, "PENDING")
                .orElseThrow(() -> new ResourceNotFoundException("Upload session", "id", sessionId.toString()));

        // Verify the object actually landed in S3
        boolean exists = s3Gateway.objectExists(session.getBucket(), session.getPresignedKey());
        if (!exists) {
            throw new com.manacommunity.api.exception.MediaStorageException(
                    "File not found in S3 bucket (" + session.getBucket() + "). Presigned upload incomplete. Database record was NOT created.");
        }

        // Determine media type and extension from session
        String ext = session.getMimeType() != null
                ? mimeDetector.toExtension(session.getMimeType())
                : "bin";

        MediaObject media = MediaObject.builder()
                .module(session.getModule())
                .moduleId(session.getModuleId())
                .communityId(session.getCommunityId())
                .subContext(session.getSubContext())
                .originalFileName(session.getOriginalName() != null ? session.getOriginalName() : "file")
                .storedFileName(session.getPresignedKey().substring(session.getPresignedKey().lastIndexOf('/') + 1))
                .mimeType(session.getMimeType() != null ? session.getMimeType() : "application/octet-stream")
                .extension(ext)
                .fileSize(session.getFileSize())
                .mediaType(session.getMediaType())
                .bucketName(session.getBucket())
                .s3Key(session.getPresignedKey())
                .uploadedBy(uploaderId)
                .status(MediaStatus.ACTIVE)
                .processingStatus("QUEUED")
                .build();

        media = mediaRepo.save(media);

        // Mark session as completed
        session.setStatus("COMPLETED");
        sessionRepo.save(session);

        log.info("Presigned upload confirmed: sessionId={} externalId={}", sessionId, media.getExternalId());

        return toResponse(media);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MediaResponse getById(UUID externalId) {
        MediaObject media = mediaRepo.findByExternalIdAndDeletedFalse(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", externalId.toString()));
        return toResponse(media);
    }

    @Transactional(readOnly = true)
    public Page<MediaResponse> getByModule(MediaModule module, String moduleId, Pageable pageable) {
        return mediaRepo.findByModuleAndModuleIdAndDeletedFalseOrderByUploadedAtDesc(module, moduleId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> getActiveByModule(MediaModule module, String moduleId) {
        return mediaRepo.findByModuleAndModuleIdAndDeletedFalseAndStatusOrderBySortOrderAscUploadedAtDesc(
                        module, moduleId, MediaStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> getByModuleAndSubContext(MediaModule module, String moduleId, String subContext) {
        return mediaRepo.findByModuleAndModuleIdAndSubContextAndDeletedFalseAndStatus(
                        module, moduleId, subContext, MediaStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Transactional
    public void softDelete(UUID externalId, Long deletedBy) {
        MediaObject media = findActive(externalId);
        media.setDeleted(true);
        media.setStatus(MediaStatus.DELETED);
        media.setDeletedBy(deletedBy);
        media.setDeletedAt(OffsetDateTime.now());
        mediaRepo.save(media);
        log.info("Media soft-deleted: externalId={} by userId={}", externalId, deletedBy);
    }

    @Transactional
    public MediaResponse restore(UUID externalId, Long restoredBy) {
        MediaObject media = mediaRepo.findByExternalIdAndDeletedFalse(externalId)
                .or(() -> mediaRepo.findByExternalIdAndDeletedFalse(externalId))
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", externalId.toString()));
        // Allow restoring deleted items
        media.setDeleted(false);
        media.setStatus(MediaStatus.ACTIVE);
        media.setDeletedBy(null);
        media.setDeletedAt(null);
        mediaRepo.save(media);
        log.info("Media restored: externalId={} by userId={}", externalId, restoredBy);
        return toResponse(media);
    }

    // ── Approval workflow ─────────────────────────────────────────────────────

    @Transactional
    public MediaResponse approve(UUID externalId, Long approverId) {
        MediaObject media = findActive(externalId);
        media.setStatus(MediaStatus.ACTIVE);
        media.setApprovedBy(approverId);
        media.setApprovedAt(OffsetDateTime.now());
        mediaRepo.save(media);
        log.info("Media approved: externalId={} by userId={}", externalId, approverId);
        return toResponse(media);
    }

    @Transactional
    public MediaResponse reject(UUID externalId, String reason, Long approverId) {
        MediaObject media = findActive(externalId);
        media.setStatus(MediaStatus.REJECTED);
        media.setRejectionReason(reason);
        media.setApprovedBy(approverId);
        media.setApprovedAt(OffsetDateTime.now());
        mediaRepo.save(media);
        log.info("Media rejected: externalId={} reason={}", externalId, reason);
        return toResponse(media);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private MediaObject findActive(UUID externalId) {
        return mediaRepo.findByExternalIdAndDeletedFalse(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", externalId.toString()));
    }

    /**
     * Map a {@link MediaObject} entity to a {@link MediaResponse} DTO,
     * generating all access URLs dynamically.
     */
    private MediaResponse toResponse(MediaObject m) {
        return MediaResponse.builder()
                .id(m.getExternalId())
                .module(m.getModule())
                .moduleId(m.getModuleId())
                .communityId(m.getCommunityId())
                .subContext(m.getSubContext())
                .mediaType(m.getMediaType())
                .fileSize(m.getFileSize())
                .mimeType(m.getMimeType())
                .originalFileName(m.getOriginalFileName())
                .caption(m.getCaption())
                .altText(m.getAltText())
                .featured(m.isFeatured())
                .sortOrder(m.getSortOrder())
                .status(m.getStatus())
                .url(urlService.generateUrl(m))
                .thumbnailUrl(urlService.generateThumbnailUrl(m))
                .compressedUrl(urlService.generateCompressedUrl(m))
                .mediumUrl(urlService.generateMediumUrl(m))
                .width(m.getWidth())
                .height(m.getHeight())
                .durationSeconds(m.getDurationSeconds())
                .uploadedBy(m.getUploadedBy())
                .uploadedAt(m.getUploadedAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private void validateSize(long bytes, MediaType type) {
        long maxBytes = switch (type) {
            case IMAGE, QR_CODE, CERTIFICATE -> (long) props.getLimits().getMaxImageSizeMb() * 1024 * 1024;
            case VIDEO, AUDIO                -> (long) props.getLimits().getMaxVideoSizeMb() * 1024 * 1024;
            default                          -> (long) props.getLimits().getMaxDocumentSizeMb() * 1024 * 1024;
        };
        if (bytes > maxBytes) {
            throw new IllegalArgumentException(
                    "File size " + (bytes / 1024 / 1024) + " MB exceeds the limit of " + (maxBytes / 1024 / 1024) + " MB for type " + type);
        }
    }
}
