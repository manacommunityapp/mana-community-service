package com.manacommunity.api.storage.impl;

import com.manacommunity.api.exception.MediaStorageException;
import com.manacommunity.api.storage.FileStorageService;
import com.manacommunity.api.storage.FileStreamResource;
import com.manacommunity.api.storage.StoredFileDto;
import com.manacommunity.api.storage.entity.StoredFile;
import com.manacommunity.api.storage.repository.StoredFileRepository;
import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Default file storage: persists binary data as BYTEA in the {@code stored_file}
 * PostgreSQL table. Files are served back through {@code GET /api/files/{id}}.
 *
 * Active when {@code app.storage.type=postgres} (or the property is absent).
 * No AWS credentials or S3 bucket are required.
 *
 * To switch to S3:
 *   1. Set {@code app.storage.type=s3} in your environment / application.yaml
 *   2. Provide {@code app.storage.s3.bucket}, {@code .region},
 *      {@code .access-key}, {@code .secret-key}
 *   3. Restart — S3FileStorageService auto-activates, this bean stays dormant
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "postgres", matchIfMissing = true)
@RequiredArgsConstructor
public class PostgresFileStorageService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20 MB

    private final StoredFileRepository repo;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public StoredFileDto store(MultipartFile file, Long uploadedByUserId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds the 20 MB limit");
        }

        try {
            String ext = extractExtension(file.getOriginalFilename());
            String uniqueName = UUID.randomUUID() + ext;

            StoredFile entity = StoredFile.builder()
                    .filename(uniqueName)
                    .originalName(sanitise(file.getOriginalFilename()))
                    .contentType(resolveContentType(file))
                    .sizeBytes(file.getSize())
                    .data(file.getBytes())
                    .uploadedBy(uploadedByUserId)
                    .communityId(resolveCommunityId(uploadedByUserId))
                    .build();

            entity = repo.save(entity);
            log.debug("Stored file id={} name={} size={}", entity.getId(), entity.getOriginalName(), entity.getSizeBytes());

            return new StoredFileDto(
                    entity.getId(),
                    "/api/files/" + entity.getId(),
                    entity.getOriginalName(),
                    entity.getContentType(),
                    entity.getSizeBytes()
            );
        } catch (IOException e) {
            throw new MediaStorageException("Failed to read uploaded file bytes", e);
        }
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        if (fileId != null) {
            repo.deleteById(fileId);
        }
    }

    @Override
    public FileStreamResource getStream(String key) {
        if (key == null || key.isBlank()) return null;
        try {
            StoredFile file = null;
            if (key.matches("^\\d+$")) {
                file = repo.findById(Long.parseLong(key)).orElse(null);
            }
            if (file == null) {
                file = repo.findByFilename(key).orElse(null);
            }
            if (file == null) return null;

            final byte[] data = file.getData();
            return FileStreamResource.builder()
                    .body(outputStream -> outputStream.write(data))
                    .contentType(file.getContentType())
                    .contentLength(file.getSizeBytes())
                    .filename(file.getOriginalName())
                    .build();
        } catch (Exception e) {
            log.error("Failed to load postgres file for key={}: {}", key, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean isS3() {
        return false;
    }

    /**
     * Owning community for a new upload (stored_file.community_id, V506).
     *
     * <p>Preferred source is {@link TenantContext}, which TenantFilter populates from the
     * authenticated principal — never from a client header for a non-SUPER_ADMIN. For a
     * SUPER_ADMIN acting on another community the context holds that community, which is the
     * right owner for the file. When there is no request context (background jobs, tests) the
     * uploader's own community is used. Null when neither is known: the row then behaves
     * exactly like a legacy row and is SUPER_ADMIN-only.
     */
    private Long resolveCommunityId(Long uploadedByUserId) {
        Long fromContext = TenantContext.getCommunityId();
        if (fromContext != null) {
            return fromContext;
        }
        if (uploadedByUserId == null) {
            return null;
        }
        return appUserRepository.findById(uploadedByUserId)
                .map(AppUser::getCommunity)
                .map(c -> c.getId())
                .orElse(null);
    }

    private String extractExtension(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf(".")).toLowerCase();
    }

    private String sanitise(String name) {
        if (name == null) return "upload";
        return name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
    }

    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct != null && !ct.isBlank()) ? ct : "application/octet-stream";
    }
}
