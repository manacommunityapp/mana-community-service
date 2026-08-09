package com.manacommunity.api.media.service;

import com.manacommunity.api.media.config.MediaProperties;
import com.manacommunity.api.media.entity.MediaObject;
import com.manacommunity.api.media.entity.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * Low-level S3 gateway for the Media Management Service.
 * All interactions with AWS S3 are routed through this class.
 *
 * Rules:
 *  - Server-side encryption (SSE-S3 AES-256) is applied to all PutObject calls.
 *  - Keys are NEVER logged in full — log only the prefix for security.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3MediaGateway {

    @Qualifier("mediaS3Client")
    private final S3Client s3Client;

    @Qualifier("mediaS3Presigner")
    private final S3Presigner presigner;

    private final MediaProperties props;

    // ── Upload ───────────────────────────────────────────────────────────────

    private final java.util.concurrent.ConcurrentHashMap<String, S3Client> regionClients = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Upload a stream directly to S3 with SSE-S3 encryption.
     * Auto-handles HTTP 301 PermanentRedirect if bucket region differs from default.
     */
    public void putObject(String bucket, String key, InputStream stream,
                          String mimeType, long contentLength) {
        log.debug("S3 PutObject bucket={} keyPrefix={}...", bucket, key.substring(0, Math.min(40, key.length())));
        byte[] bytes;
        try {
            bytes = stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream bytes", e);
        }

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(mimeType)
                            .contentLength(contentLength)
                            .serverSideEncryption(ServerSideEncryption.AES256)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
        } catch (S3Exception e) {
            if (e.statusCode() == 301) {
                String targetRegion = extractRegionFromException(e);
                log.warn("S3 301 PermanentRedirect encountered for bucket {}. Auto-redirecting to region {}...", bucket, targetRegion);
                S3Client targetClient = getClientForRegion(targetRegion);
                targetClient.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(mimeType)
                                .contentLength(contentLength)
                                .serverSideEncryption(ServerSideEncryption.AES256)
                                .build(),
                        RequestBody.fromBytes(bytes)
                );
                log.info("S3 PutObject complete via redirected client for bucket={} region={}", bucket, targetRegion);
                return;
            }
            throw e;
        }
        log.info("S3 PutObject complete bucket={} keyPrefix={}...", bucket, key.substring(0, Math.min(40, key.length())));
    }

    private String extractRegionFromException(S3Exception e) {
        if (e.awsErrorDetails() != null && e.awsErrorDetails().sdkHttpResponse() != null) {
            var headers = e.awsErrorDetails().sdkHttpResponse().headers();
            if (headers != null && headers.containsKey("x-amz-bucket-region")) {
                var list = headers.get("x-amz-bucket-region");
                if (list != null && !list.isEmpty()) {
                    return list.get(0);
                }
            }
        }
        return props.getS3().getRegion() != null ? props.getS3().getRegion() : "ap-south-1";
    }

    private S3Client getClientForRegion(String regionName) {
        return regionClients.computeIfAbsent(regionName, r -> S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(r))
                .crossRegionAccessEnabled(true)
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                                props.getS3().getAccessKey(),
                                props.getS3().getSecretKey())))
                .build());
    }

    /**
     * Generate a presigned PUT URL for direct browser → S3 upload.
     * Client must include Content-Type header matching the signed type.
     */
    public PresignedPutObjectRequest presignPut(String bucket, String key,
                                                 String mimeType, Duration expiry) {
        log.debug("Presigning PUT bucket={} mimeType={} expiry={}", bucket, mimeType, expiry);
        return presigner.presignPutObject(b -> b
                .putObjectRequest(r -> r
                        .bucket(bucket)
                        .key(key)
                        .contentType(mimeType))
                .signatureDuration(expiry));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    /**
     * Permanently delete an object from S3.
     * Called only after soft-delete is confirmed and admin approves physical removal.
     */
    public void deleteObject(String bucket, String key) {
        log.info("S3 DeleteObject bucket={} keyPrefix={}...", bucket, key.substring(0, Math.min(40, key.length())));
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    // ── Presigned GET ─────────────────────────────────────────────────────────

    /**
     * Generate a presigned GET URL (used when CloudFront is not configured).
     */
    public String presignGet(String bucket, String key, Duration expiry) {
        return presigner.presignGetObject(b -> b
                .getObjectRequest(r -> r.bucket(bucket).key(key))
                .signatureDuration(expiry))
                .url()
                .toString();
    }

    // ── Head / Existence check ────────────────────────────────────────────────

    public boolean objectExists(String bucket, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
