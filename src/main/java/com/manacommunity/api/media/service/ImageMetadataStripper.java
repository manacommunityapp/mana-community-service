package com.manacommunity.api.media.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Strips EXIF and other metadata from image files by re-encoding them.
 *
 * <p>Re-encoding through Java's {@link ImageIO} produces a clean image
 * with no metadata segments (EXIF, IPTC, XMP). This removes GPS coordinates,
 * camera serial numbers, and other potentially sensitive data before storage.</p>
 *
 * <p>Only JPEG and PNG are re-encoded (the two formats where metadata leakage
 * is most common). WebP, AVIF, SVG and other formats pass through unchanged
 * since either ImageIO doesn't support them or they rarely carry sensitive metadata.</p>
 */
@Slf4j
@Component
public class ImageMetadataStripper {

    private static final Set<String> STRIPPABLE_MIMES = Set.of("image/jpeg", "image/png");

    /**
     * Returns a clean {@link InputStream} with metadata stripped if the MIME type
     * supports it. For unsupported types, returns the original file's stream.
     *
     * @param file         the uploaded image
     * @param detectedMime the Tika-detected MIME type
     * @return a pair of (clean stream, byte length) — caller must close the stream
     */
    public StrippedImage strip(MultipartFile file, String detectedMime) throws IOException {
        if (!STRIPPABLE_MIMES.contains(detectedMime)) {
            return new StrippedImage(file.getInputStream(), file.getSize());
        }

        String formatName = "image/jpeg".equals(detectedMime) ? "JPEG" : "PNG";

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            log.warn("ImageIO could not decode {}; returning original bytes", file.getOriginalFilename());
            return new StrippedImage(file.getInputStream(), file.getSize());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) file.getSize());

        if ("JPEG".equals(formatName)) {
            writeJpeg(image, baos);
        } else {
            ImageIO.write(image, formatName, baos);
        }

        byte[] clean = baos.toByteArray();
        log.debug("Stripped metadata from {} ({} → {} bytes)", file.getOriginalFilename(), file.getSize(), clean.length);
        return new StrippedImage(new ByteArrayInputStream(clean), clean.length);
    }

    private void writeJpeg(BufferedImage image, ByteArrayOutputStream out) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            ImageIO.write(image, "JPEG", out);
            return;
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.92f);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            }
        } finally {
            writer.dispose();
        }
    }

    public record StrippedImage(InputStream stream, long size) {}
}
