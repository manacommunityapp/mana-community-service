package com.manacommunity.api.media.service;

import com.manacommunity.api.media.config.MediaProperties;
import com.manacommunity.api.media.entity.MediaObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Generates access URLs for media objects.
 *
 * Strategy:
 *  1. If {@code app.media.cloudfront.domain} is configured → return a plain
 *     CloudFront URL (unsigned, relies on CF behaviour policies for access control).
 *  2. Otherwise → generate a presigned S3 GET URL valid for the configured expiry.
 *
 * URLs are NEVER stored in the database. They are generated here at read time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUrlService {

    private final MediaProperties props;
    private final S3MediaGateway s3Gateway;

    /**
     * Generate the primary access URL for a media object.
     */
    public String generateUrl(MediaObject media) {
        return buildUrl(media.getBucketName(), media.getS3Key());
    }

    /**
     * Generate the thumbnail access URL. Returns null if no thumbnail exists yet.
     */
    public String generateThumbnailUrl(MediaObject media) {
        if (media.getThumbnailKey() == null || media.getThumbnailKey().isBlank()) return null;
        return buildUrl(media.getBucketName(), media.getThumbnailKey());
    }

    /**
     * Generate the compressed version access URL. Returns null if not processed yet.
     */
    public String generateCompressedUrl(MediaObject media) {
        if (media.getCompressedKey() == null || media.getCompressedKey().isBlank()) return null;
        return buildUrl(media.getBucketName(), media.getCompressedKey());
    }

    /**
     * Generate the medium (1920px) access URL. Returns null if not processed yet.
     */
    public String generateMediumUrl(MediaObject media) {
        if (media.getMediumKey() == null || media.getMediumKey().isBlank()) return null;
        return buildUrl(media.getBucketName(), media.getMediumKey());
    }

    /**
     * Resolve a URL that was persisted as a plain string (e.g. AppUser.profilePicUrl).
     * If it points at our private S3 bucket (raw, or a previously presigned/expired URL),
     * the object key is extracted and a fresh access URL is generated.
     * Any other value (null, data URI, external URL) is returned unchanged.
     */
    public String resolveStoredUrl(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        try {
            if (!stored.startsWith("http")) return stored;
            java.net.URI uri = java.net.URI.create(stored);
            String host = uri.getHost();
            String bucket = props.getS3().getBucket();
            if (host == null || bucket == null || !host.endsWith(".amazonaws.com")) return stored;

            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            String key;
            if (host.startsWith(bucket + ".s3")) {
                key = path.startsWith("/") ? path.substring(1) : path;           // virtual-hosted style
            } else if (host.startsWith("s3") && path.startsWith("/" + bucket + "/")) {
                key = path.substring(bucket.length() + 2);                        // path style
            } else {
                return stored;
            }
            if (key.isBlank()) return stored;
            key = java.net.URLDecoder.decode(key.replace("+", "%2B"), java.nio.charset.StandardCharsets.UTF_8);
            return buildUrl(bucket, key);
        } catch (Exception e) {
            log.warn("Could not resolve stored media URL: {}", e.getMessage());
            return stored;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildUrl(String bucket, String key) {
        String cfDomain = props.getCloudfront().getDomain();
        if (cfDomain != null && !cfDomain.isBlank()) {
            // CloudFront URL — fast, cached at edge, no expiry needed
            return "https://" + cfDomain + "/" + key;
        }
        // Presigned S3 GET URL — expires after configured duration
        int expiryMinutes = props.getS3().getPresignedGetExpiryMinutes();
        return s3Gateway.presignGet(bucket, key, Duration.ofMinutes(expiryMinutes));
    }
}
