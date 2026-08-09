package com.manacommunity.api.media.service;

import com.manacommunity.api.media.dto.PresignedUploadRequest;
import com.manacommunity.api.media.entity.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Validates images and video files prior to uploading to S3 or issuing presigned URLs.
 *
 * Rules:
 *  1. Non-zero byte check.
 *  2. Deep image validation via {@link ImageIO}: attempts to decode image headers.
 *     If ImageIO returns null or fails, the file is rejected as corrupted or spoofed.
 *  3. Video MIME type whitelist validation.
 *  4. Presigned request consistency check (mediaType vs declared mimeType).
 */
@Component
@Slf4j
public class MediaValidator {

    private static final Set<String> ALLOWED_IMAGE_MIMES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/avif", "image/svg+xml"
    );

    private static final Set<String> ALLOWED_VIDEO_MIMES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo", "video/3gpp", "video/ogg"
    );

    public record ImageDimensions(int width, int height) {}

    /**
     * Deeply validates an image file by reading its binary header structure.
     * Extracts width and height if valid.
     *
     * @throws IllegalArgumentException if the image is empty, unreadable, or corrupted.
     */
    public ImageDimensions validateAndExtractImageDimensions(MultipartFile file, String detectedMime) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("Image file is empty or missing.");
        }

        if (!ALLOWED_IMAGE_MIMES.contains(detectedMime)) {
            throw new IllegalArgumentException("Unsupported image format: " + detectedMime
                    + ". Allowed formats: JPG, PNG, GIF, WebP, BMP, AVIF, SVG.");
        }

        // SVG files cannot be parsed by ImageIO — return default dimensions
        if ("image/svg+xml".equalsIgnoreCase(detectedMime)) {
            return new ImageDimensions(0, 0);
        }

        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                log.warn("ImageIO returned null for file {}. File appears corrupted or is not a valid image.",
                        file.getOriginalFilename());
                throw new IllegalArgumentException(
                        "File '" + file.getOriginalFilename() + "' is corrupted or not a valid image format.");
            }

            int w = image.getWidth();
            int h = image.getHeight();

            if (w <= 0 || h <= 0) {
                throw new IllegalArgumentException("Image file has invalid dimensions: " + w + "x" + h);
            }

            log.debug("Image validated successfully: {} ({}x{})", file.getOriginalFilename(), w, h);
            return new ImageDimensions(w, h);

        } catch (IOException e) {
            log.error("Failed to read image stream for {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new IllegalArgumentException("Failed to decode image file: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a video file MIME type and size.
     *
     * @throws IllegalArgumentException if the video format is unsupported or file is empty.
     */
    public void validateVideoFile(MultipartFile file, String detectedMime) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("Video file is empty or missing.");
        }

        if (!ALLOWED_VIDEO_MIMES.contains(detectedMime)) {
            throw new IllegalArgumentException("Unsupported video format: " + detectedMime
                    + ". Allowed formats: MP4, WebM, MOV, AVI, 3GP, OGG.");
        }

        log.debug("Video validated successfully: {} ({})", file.getOriginalFilename(), detectedMime);
    }

    /**
     * Validates a presigned upload request for consistency.
     */
    public void validatePresignedRequest(PresignedUploadRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Presigned upload request cannot be null.");
        }

        String mime = request.getMimeType();
        MediaType type = request.getMediaType();

        if (mime == null || mime.isBlank()) {
            throw new IllegalArgumentException("MIME type is required.");
        }

        if (type == MediaType.IMAGE && !mime.startsWith("image/")) {
            throw new IllegalArgumentException("Declared mediaType 'IMAGE' conflicts with MIME type '" + mime + "'.");
        }

        if (type == MediaType.VIDEO && !mime.startsWith("video/")) {
            throw new IllegalArgumentException("Declared mediaType 'VIDEO' conflicts with MIME type '" + mime + "'.");
        }

        if (type == MediaType.IMAGE && !ALLOWED_IMAGE_MIMES.contains(mime)) {
            throw new IllegalArgumentException("Unsupported image MIME type: " + mime);
        }

        if (type == MediaType.VIDEO && !ALLOWED_VIDEO_MIMES.contains(mime)) {
            throw new IllegalArgumentException("Unsupported video MIME type: " + mime);
        }
    }
}
