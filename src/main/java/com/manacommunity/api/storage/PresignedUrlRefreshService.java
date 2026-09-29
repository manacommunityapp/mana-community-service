package com.manacommunity.api.storage;

import com.manacommunity.api.storage.impl.S3FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects expired AWS S3 pre-signed URLs and regenerates them from the
 * embedded S3 key. Safe to call on any URL — non-S3 and still-valid URLs
 * are returned unchanged.
 */
@Service
@Slf4j
public class PresignedUrlRefreshService {

    private static final Pattern AMZ_DATE = Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})Z$");
    private static final DateTimeFormatter ISO_BASIC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final S3FileStorageService s3Storage;

    @Autowired(required = false)
    public PresignedUrlRefreshService(@Autowired(required = false) S3FileStorageService s3Storage) {
        this.s3Storage = s3Storage;
    }

    /**
     * If the URL is an expired (or near-expiry) S3 pre-signed URL,
     * generate a fresh one from the S3 key. Otherwise return as-is.
     *
     * @param url any URL string (may be null, blank, data-URI, non-S3, etc.)
     * @return a fresh pre-signed URL, or the original unchanged
     */
    public String refreshIfExpired(String url) {
        if (url == null || url.isBlank()) return url;
        if (!url.contains("X-Amz-Date")) return url;
        if (s3Storage == null) return url;

        try {
            URI uri = URI.create(url);
            String query = uri.getRawQuery();
            if (query == null) return url;

            String amzDate = extractParam(query, "X-Amz-Date");
            String amzExpires = extractParam(query, "X-Amz-Expires");
            if (amzDate == null) return url;

            Matcher m = AMZ_DATE.matcher(amzDate);
            if (!m.matches()) return url;

            Instant signedAt = Instant.parse(
                    m.group(1) + "-" + m.group(2) + "-" + m.group(3) +
                    "T" + m.group(4) + ":" + m.group(5) + ":" + m.group(6) + "Z"
            );
            long expirySec = amzExpires != null ? Long.parseLong(amzExpires) : 3600;
            Instant expiresAt = signedAt.plusSeconds(expirySec);

            // Refresh if expired or within 5 minutes of expiry
            if (Instant.now().isAfter(expiresAt.minusSeconds(300))) {
                String key = uri.getPath();
                if (key.startsWith("/")) key = key.substring(1);
                String fresh = s3Storage.generatePresignedGetUrl(key);
                log.debug("Refreshed expired pre-signed URL for key={}", key);
                return fresh;
            }
        } catch (Exception e) {
            log.warn("Failed to refresh pre-signed URL, returning original: {}", e.getMessage());
        }
        return url;
    }

    private static String extractParam(String query, String name) {
        String prefix = name + "=";
        int idx = query.indexOf(prefix);
        if (idx < 0) return null;
        int start = idx + prefix.length();
        int end = query.indexOf('&', start);
        return end < 0 ? query.substring(start) : query.substring(start, end);
    }
}
