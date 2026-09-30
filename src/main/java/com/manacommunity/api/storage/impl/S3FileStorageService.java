package com.manacommunity.api.storage.impl;

import com.manacommunity.api.storage.FileStorageService;
import com.manacommunity.api.storage.StoredFileDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.UUID;

/**
 * S3-backed file storage. Active when {@code app.storage.type=s3}.
 *
 * <p>Required properties:
 * <pre>
 *   app.storage.s3.bucket                        — S3 bucket name
 *   app.storage.s3.region                        — AWS region (e.g. eu-north-1)
 *   app.storage.s3.access-key                    — AWS access key ID
 *   app.storage.s3.secret-key                    — AWS secret access key
 *   app.storage.s3.presigned-get-expiry-minutes  — URL expiry (default 60 min)
 * </pre>
 *
 * <p>Files are stored as <strong>private</strong> S3 objects. Download URLs are
 * time-limited presigned GET URLs instead of permanent public URLs.
 *
 * <p><strong>IMPORTANT:</strong> Ensure the S3 bucket has "Block All Public Access"
 * enabled in the AWS Console to enforce this policy at the infrastructure level.
 */
@Slf4j
@Primary
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20 MB

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    @Value("${app.storage.s3.region}")
    private String region;

    @Value("${app.storage.s3.access-key}")
    private String accessKey;

    @Value("${app.storage.s3.secret-key}")
    private String secretKey;

    @Value("${app.storage.s3.presigned-get-expiry-minutes:60}")
    private int presignedGetExpiryMinutes;

    private S3Client s3;
    private S3Presigner presigner;

    @PostConstruct
    void init() {
        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
        Region awsRegion = Region.of(region);

        s3 = S3Client.builder()
                .region(awsRegion)
                .crossRegionAccessEnabled(true)
                .credentialsProvider(creds)
                .build();

        presigner = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(creds)
                .build();

        log.info("S3FileStorageService initialised — bucket={} region={} presignedExpiry={}min",
                bucket, region, presignedGetExpiryMinutes);
    }

    @Override
    public StoredFileDto store(MultipartFile file, Long uploadedByUserId) {
        return store(file, uploadedByUserId, null);
    }

    @Override
    public StoredFileDto store(MultipartFile file, Long uploadedByUserId, String customPath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds the 20 MB limit");
        }

        try {
            String ext = extractExtension(file.getOriginalFilename());
            String key;
            if (customPath != null && !customPath.isBlank()) {
                String cleanPath = customPath.trim();
                if (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1);
                if (cleanPath.endsWith("/")) cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
                String fileName = "file_" + System.currentTimeMillis() + ext;
                key = cleanPath + "/" + fileName;
            } else {
                key = "uploads/" + UUID.randomUUID() + ext;
            }
            String contentType = resolveContentType(file);

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            s3.putObject(put, RequestBody.fromBytes(file.getBytes()));

            // Generate a presigned GET URL instead of a permanent public URL.
            // The URL expires after presignedGetExpiryMinutes (default: 60 minutes).
            String presignedUrl = generatePresignedGetUrl(key);

            log.debug("Uploaded to S3 key={} size={} presignedExpiry={}min",
                    key, file.getSize(), presignedGetExpiryMinutes);

            return new StoredFileDto(
                    null,
                    presignedUrl,
                    sanitise(file.getOriginalFilename()),
                    contentType,
                    file.getSize()
            );
        } catch (Exception e) {
            log.error("Failed to upload to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a presigned GET URL for a private S3 object.
     * The URL is valid for {@code presignedGetExpiryMinutes} minutes.
     *
     * @param key the S3 object key
     * @return a time-limited pre-signed GET URL
     */
    public String generatePresignedGetUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedGetExpiryMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(Long fileId) {
        // S3 deletion uses the object key, not a DB id.
        // Use deleteByKey(String) to delete an object by its S3 key.
        log.warn("S3 delete by id is a no-op — use deleteByKey(String key) instead");
    }

    /** Deletes an S3 object by its key (path inside the bucket). */
    public void deleteByKey(String key) {
        if (key == null || key.isBlank()) return;
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        log.debug("Deleted S3 object key={}", key);
    }

    @Override
    public boolean isS3() {
        return true;
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
