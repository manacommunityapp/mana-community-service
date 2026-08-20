package com.manacommunity.api.media.service;

import com.manacommunity.api.media.entity.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Detects the true MIME type from file bytes using Apache Tika.
 * NEVER trust the client-supplied Content-Type header — it can be spoofed.
 *
 * Also validates that the detected type is on the allowlist, and maps
 * MIME types to file extensions and {@link MediaType} classifications.
 */
@Component
@Slf4j
public class MimeTypeDetector {

    private static final Tika TIKA = new Tika();

    /** MIME type → normalised extension. */
    private static final Map<String, String> MIME_TO_EXT = Map.ofEntries(
            Map.entry("image/jpeg",       "jpg"),
            Map.entry("image/png",        "png"),
            Map.entry("image/gif",        "gif"),
            Map.entry("image/webp",       "webp"),
            Map.entry("image/avif",       "avif"),
            Map.entry("image/bmp",        "bmp"),
            Map.entry("video/mp4",        "mp4"),
            Map.entry("video/webm",       "webm"),
            Map.entry("video/quicktime",  "mov"),
            Map.entry("video/x-msvideo", "avi"),
            Map.entry("audio/mpeg",       "mp3"),
            Map.entry("audio/wav",        "wav"),
            Map.entry("audio/ogg",        "ogg"),
            Map.entry("application/pdf",  "pdf"),
            Map.entry("application/msword", "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-excel", "xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
            Map.entry("application/vnd.ms-powerpoint", "ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
            Map.entry("image/svg+xml",    "svg")
    );

    /** MIME type → MediaType classification. */
    private static final Map<String, MediaType> MIME_TO_MEDIA_TYPE = Map.ofEntries(
            Map.entry("image/jpeg",  MediaType.IMAGE),
            Map.entry("image/png",   MediaType.IMAGE),
            Map.entry("image/gif",   MediaType.IMAGE),
            Map.entry("image/webp",  MediaType.IMAGE),
            Map.entry("image/avif",  MediaType.IMAGE),
            Map.entry("image/bmp",   MediaType.IMAGE),
            Map.entry("image/svg+xml", MediaType.IMAGE),
            Map.entry("video/mp4",       MediaType.VIDEO),
            Map.entry("video/webm",      MediaType.VIDEO),
            Map.entry("video/quicktime", MediaType.VIDEO),
            Map.entry("video/x-msvideo",MediaType.VIDEO),
            Map.entry("audio/mpeg",  MediaType.AUDIO),
            Map.entry("audio/wav",   MediaType.AUDIO),
            Map.entry("audio/ogg",   MediaType.AUDIO),
            Map.entry("application/pdf",  MediaType.DOCUMENT),
            Map.entry("application/msword", MediaType.DOCUMENT),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", MediaType.DOCUMENT),
            Map.entry("application/vnd.ms-excel", MediaType.DOCUMENT),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", MediaType.DOCUMENT),
            Map.entry("application/vnd.ms-powerpoint", MediaType.DOCUMENT),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", MediaType.DOCUMENT)
    );

    /**
     * Detect the actual MIME type from file bytes (ignores client Content-Type).
     *
     * @throws IllegalArgumentException if the type is not on the allowlist
     */
    public String detect(MultipartFile file) {
        try {
            String detected = TIKA.detect(file.getInputStream(), file.getOriginalFilename());
            log.debug("Tika detected MIME type: {} for file: {}", detected, file.getOriginalFilename());

            if (!MIME_TO_EXT.containsKey(detected)) {
                throw new IllegalArgumentException(
                        "Unsupported file type: " + detected + ". Allowed: images, videos, audio, PDF, Office documents.");
            }
            return detected;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file for MIME detection: " + e.getMessage(), e);
        }
    }

    /** Map a MIME type to a safe file extension. */
    public String toExtension(String mimeType) {
        return MIME_TO_EXT.getOrDefault(mimeType, "bin");
    }

    /** Classify the MIME type into a {@link MediaType} enum value. */
    public MediaType toMediaType(String mimeType) {
        return MIME_TO_MEDIA_TYPE.getOrDefault(mimeType, MediaType.DOCUMENT);
    }
}
