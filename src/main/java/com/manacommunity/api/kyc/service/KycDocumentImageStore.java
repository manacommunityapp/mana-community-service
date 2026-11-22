package com.manacommunity.api.kyc.service;

import com.manacommunity.api.exception.InvalidFileUploadException;
import com.manacommunity.api.exception.MediaStorageException;
import com.manacommunity.api.storage.FileStorageService;
import com.manacommunity.api.storage.StoredFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns the base64 document images on {@code KycSubmitRequest} into stored files and
 * returns the URLs that go onto {@code kyc_document.front_image_url / back_image_url}.
 *
 * <h3>Why this exists</h3>
 * {@code frontImageBase64} / {@code backImageBase64} were bound from the request body and
 * then read by nothing at all, so every KYC submission stored a digest and the last four
 * characters and discarded the evidence. The image URL columns stayed NULL, which left the
 * admin review screen with nothing to review — the flow looked like it worked and did not.
 *
 * <h3>Which storage backend</h3>
 * {@link FileStorageService}, not {@code MediaService}. Three reasons:
 * <ul>
 *   <li>{@code MediaService.uploadMedia} only ever writes to S3 and throws when the bucket
 *       is unreachable; this deployment defaults to {@code app.storage.type=postgres}, where
 *       there is no bucket at all. {@code FileStorageService} covers both backends behind one
 *       interface, so KYC works in the default configuration and follows the deployment to S3
 *       without a code change.</li>
 *   <li>{@code MediaService} deliberately never persists a URL (it regenerates presigned URLs
 *       on demand), whereas {@code KycDocument} has two persisted URL columns to fill.
 *       {@code StoredFileDto.url} is exactly that: a stable {@code /api/files/{id}}.</li>
 *   <li>Tenancy: {@code PostgresFileStorageService} stamps {@code stored_file.community_id}
 *       from {@code TenantContext} (V506), and {@code GET /api/files/{id}} enforces
 *       uploader-or-same-community-or-SUPER_ADMIN on it. The document therefore lands scoped
 *       to the resident's community rather than world-readable.</li>
 * </ul>
 *
 * <h3>Why the key path is {@code users/{userId}/kyc/...}</h3>
 * The custom path is only used by the S3 backend, where files are served through the wildcard
 * route {@code GET /api/files/**}. That route can only scope a key it recognises:
 * {@code FileStorageController#canAccessKey} resolves {@code users/{userId}/...} to its owner
 * and checks the caller, but a key it cannot attribute to any row (an {@code events/...}-style
 * prefix, for instance) falls through to "authenticated, no tenant check". Putting KYC
 * documents in the per-user namespace means the existing check scopes them; a
 * {@code kyc/{userId}/...} prefix would read as unattributable and be served to any logged-in
 * caller. The document type is kept as the next segment so the object stays identifiable.
 *
 * <h3>Validation</h3>
 * The decoded bytes are capped at 5 MB per image — comfortably more than a phone photo of an
 * Aadhaar card, well under {@code PostgresFileStorageService}'s own 20 MB ceiling, and small
 * enough that two of them in one JSON body cannot be used to exhaust heap. The content type is
 * detected from the bytes with Tika and must be one of JPEG / PNG / WebP / HEIC / HEIF; the
 * client's claim in a {@code data:} prefix is ignored. SVG is rejected even though it is an
 * image type: {@code GET /api/files/{id}} serves the stored content type with
 * {@code Content-Disposition: inline}, so a stored SVG would be script executing on the API
 * origin. Anything else is a 400 and nothing is written.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycDocumentImageStore {

    /** Maximum decoded size of a single document image. */
    static final long MAX_DECODED_BYTES = 5L * 1024 * 1024;

    /**
     * Ceiling on the encoded string, checked before decoding so an oversized payload is
     * rejected without materialising it. Base64 is 4 characters per 3 bytes; the slack
     * absorbs a {@code data:} prefix and line breaks.
     */
    private static final int MAX_ENCODED_CHARS = (int) (((MAX_DECODED_BYTES + 2) / 3) * 4) + 4096;

    /**
     * Raster image types a government ID photo can legitimately be. SVG is deliberately
     * absent — see the class comment.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic", "image/heif");

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/heic", ".heic",
            "image/heif", ".heif");

    private static final Tika TIKA = new Tika();

    private final FileStorageService storageService;

    /**
     * Decodes, validates and stores one document image.
     *
     * @param base64       the base64 payload, with or without a {@code data:...;base64,} prefix
     * @param fieldName    the JSON field name, for error messages
     * @param documentType the KYC document type, used as a path segment
     * @param userId       the submitting resident; owner of the stored file
     * @param side         {@code front} or {@code back}, used in the filename
     * @return the URL to persist, or {@code null} when no image was supplied
     * @throws InvalidFileUploadException (400) when the payload is not a valid, small enough image
     * @throws MediaStorageException     (5xx) when the storage backend refuses the file
     */
    public String store(String base64, String fieldName, String documentType, Long userId, String side) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }

        byte[] bytes = decode(base64, fieldName);
        String contentType = detectImageContentType(bytes, fieldName);

        String filename = "kyc-" + sanitise(documentType) + "-" + side + "-"
                + UUID.randomUUID() + EXTENSIONS.get(contentType);
        String customPath = "users/" + userId + "/kyc/" + sanitise(documentType);

        try {
            StoredFileDto stored = storageService.store(
                    new InMemoryMultipartFile(filename, contentType, bytes), userId, customPath);
            if (stored == null || stored.url() == null || stored.url().isBlank()) {
                // A backend that returns no URL has given us nothing to put on the row, which
                // is the exact failure this class exists to remove. Treat it as a failure.
                throw new MediaStorageException(
                        "Storage returned no URL for the KYC " + side + " image. Nothing was recorded.");
            }
            log.info("Stored KYC {} image for userId={} as {} ({} bytes, {})",
                    side, userId, stored.url(), bytes.length, contentType);
            return stored.url();
        } catch (InvalidFileUploadException | MediaStorageException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            // The backend's own size/empty checks.
            throw new InvalidFileUploadException("'" + fieldName + "' was rejected by storage: " + e.getMessage());
        } catch (RuntimeException e) {
            // Must not degrade into a KYC row with a NULL image URL: let it abort the
            // transaction so the submission fails as a whole.
            log.error("Failed to store KYC {} image for userId={}: {}", side, userId, e.toString());
            throw new MediaStorageException(
                    "Could not store the KYC " + side + " image. The submission was not recorded — "
                            + "please try again.", e);
        }
    }

    private byte[] decode(String base64, String fieldName) {
        String payload = base64.trim();

        // Strip an optional data URI prefix. The media type it claims is not trusted; the
        // bytes are sniffed below.
        if (payload.startsWith("data:")) {
            int comma = payload.indexOf(',');
            if (comma < 0) {
                throw new InvalidFileUploadException(
                        "'" + fieldName + "' looks like a data URI but has no ',' separating the "
                                + "header from the base64 payload.");
            }
            payload = payload.substring(comma + 1);
        }

        if (payload.length() > MAX_ENCODED_CHARS) {
            throw new InvalidFileUploadException(
                    "'" + fieldName + "' is too large: the decoded image must not exceed "
                            + (MAX_DECODED_BYTES / (1024 * 1024)) + " MB.");
        }

        byte[] bytes;
        try {
            // MIME decoder: tolerates the line breaks a client may wrap the payload in.
            bytes = Base64.getMimeDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new InvalidFileUploadException("'" + fieldName + "' is not valid base64.");
        }

        if (bytes.length == 0) {
            throw new InvalidFileUploadException("'" + fieldName + "' decoded to an empty file.");
        }
        if (bytes.length > MAX_DECODED_BYTES) {
            throw new InvalidFileUploadException(
                    "'" + fieldName + "' is too large: " + bytes.length + " bytes decoded, limit is "
                            + (MAX_DECODED_BYTES / (1024 * 1024)) + " MB.");
        }
        return bytes;
    }

    /** Content type sniffed from the bytes, restricted to the raster image allowlist. */
    private String detectImageContentType(byte[] bytes, String fieldName) {
        String detected = TIKA.detect(bytes);
        String normalised = detected == null ? "" : detected.toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_CONTENT_TYPES.contains(normalised)) {
            throw new InvalidFileUploadException(
                    "'" + fieldName + "' must be a JPEG, PNG, WebP or HEIC image; the bytes supplied "
                            + "are " + (normalised.isEmpty() ? "unrecognised" : normalised) + ". A KYC "
                            + "document image is the only thing accepted here.");
        }
        return normalised;
    }

    private String sanitise(String segment) {
        if (segment == null || segment.isBlank()) return "document";
        String clean = segment.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("^-+|-+$", "");
        return clean.isBlank() ? "document" : clean;
    }

    /**
     * Minimal {@link MultipartFile} over a byte array, so a base64 body can be handed to the
     * multipart-shaped {@link FileStorageService} without a temp file or a servlet upload.
     */
    private record InMemoryMultipartFile(String filename, String contentType, byte[] bytes)
            implements MultipartFile {

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }

        @Override
        public void transferTo(Path dest) throws IOException {
            Files.write(dest, bytes);
        }
    }
}
