package com.manacommunity.api.document.service;

import com.manacommunity.api.document.entity.DocumentCategory;
import com.manacommunity.api.media.service.S3MediaGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentS3Service {

    private final S3MediaGateway s3Gateway;

    public String buildS3Key(Long communityId, DocumentCategory category,
                              Long documentId, int version, String filename) {
        String safe = filename == null ? "file" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("community/%d/documents/%s/%d/v%d/%s",
                communityId, category.name().toLowerCase(), documentId, version, safe);
    }

    public void upload(String s3Key, String bucket, InputStream content,
                       long size, String contentType) {
        s3Gateway.putObject(bucket, s3Key, content, contentType, size);
    }

    public String generatePresignedUrl(String s3Key, String bucket, Duration expiry) {
        return s3Gateway.presignGet(bucket, s3Key, expiry);
    }

    public void deleteObject(String s3Key, String bucket) {
        s3Gateway.deleteObject(bucket, s3Key);
    }
}
