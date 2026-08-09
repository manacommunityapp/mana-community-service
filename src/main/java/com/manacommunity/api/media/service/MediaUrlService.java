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
