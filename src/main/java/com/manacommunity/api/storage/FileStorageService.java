package com.manacommunity.api.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over file storage backends.
 *
 * Two implementations exist:
 *  - {@link impl.PostgresFileStorageService} — stores binary data in the
 *    {@code stored_file} PostgreSQL table. Active by default
 *    ({@code app.storage.type=postgres} or property absent).
 *    Files are served back through {@code GET /api/files/{id}}.
 *
 *  - {@link impl.S3FileStorageService} — uploads to an AWS S3 bucket and
 *    returns the public/presigned URL directly. Activate by setting:
 *    {@code app.storage.type=s3} plus the four S3 properties in
 *    {@code application.yaml}. No code changes required — swap the property
 *    and restart.
 */
public interface FileStorageService {

    /**
     * Store a file and return metadata including the URL that callers should
     * persist (e.g. on {@code event_invoice.invoice_url}).
     *
     * @param file           the uploaded multipart file
     * @param uploadedByUserId the authenticated user's id (may be null for public uploads)
     * @return dto with resolved URL and basic metadata
     */
    StoredFileDto store(MultipartFile file, Long uploadedByUserId);

    /**
     * Store a file with a custom hierarchical S3 folder path prefix.
     */
    default StoredFileDto store(MultipartFile file, Long uploadedByUserId, String customPath) {
        return store(file, uploadedByUserId);
    }

    /**
     * Delete a previously stored file. No-op if the id does not exist.
     * For S3 files the id is ignored — pass the full URL via a separate call
     * if you need S3 deletion (not implemented by default; safe to extend).
     */
    void delete(Long fileId);

    /** True when this implementation stores files externally (S3). */
    boolean isS3();
}
