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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

/**
 * S3-backed file storage. Active when {@code app.storage.type=s3}.
 *
 * Required properties:
 *   app.storage.s3.bucket      — S3 bucket name
 *   app.storage.s3.region      — AWS region (e.g. ap-south-1)
 *   app.storage.s3.access-key  — AWS access key ID
 *   app.storage.s3.secret-key  — AWS secret access key
 *
 * Files are stored as public objects; the returned URL is the S3 object URL.
 * To use presigned URLs instead, replace the URL construction with a presigner call.
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

    private S3Client s3;

    @PostConstruct
    void init() {
        s3 = S3Client.builder()
                .region(Region.of(region))
                .crossRegionAccessEnabled(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        log.info("S3FileStorageService initialised — bucket={} region={}", bucket, region);
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
                String fileName = "payment_screenshot_" + System.currentTimeMillis() + ext;
                key = cleanPath + "/" + fileName;
            } else {
                key = "invoices/" + UUID.randomUUID() + ext;
            }
            String contentType = resolveContentType(file);

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            s3.putObject(put, RequestBody.fromBytes(file.getBytes()));

            String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
            log.debug("Uploaded to S3 key={} size={}", key, file.getSize());

            return new StoredFileDto(
                    null,
                    url,
                    sanitise(file.getOriginalFilename()),
                    contentType,
                    file.getSize()
            );
        } catch (Exception e) {
            log.error("Failed to upload to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long fileId) {
        // S3 deletion uses the object URL/key, not a DB id.
        // Extend this method to accept a key String if needed.
        log.warn("S3 delete by id is a no-op — pass the S3 key to delete an object");
    }

    /** Delete an S3 object by its key (path inside the bucket). */
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
