package com.manacommunity.api.media.service;

import com.manacommunity.api.media.config.MediaProperties;
import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.entity.MediaType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Generates deterministic, UUID-based S3 object keys.
 *
 * Rules:
 *  - NEVER use original file names in the S3 key.
 *  - Always include UUID + epoch millis for uniqueness.
 *  - Key structure reflects the module/context hierarchy for easy lifecycle management.
 *
 * Key pattern (without title):
 *   {module}/{communityId}/{moduleId}/{subContext}/original/{uuid}_{epochMs}.{ext}
 *
 * Key pattern (with title slug):
 *   {module}/{communityId}/{moduleId}/{title-slug}/{subContext}/original/{uuid}_{epochMs}.{ext}
 *
 * Examples:
 *   events/1001/EVT100001/visarjan_procession/gallery/original/2f8d7f4a_1722975600000.webp
 *   users/5001/profile/original/a3f89c21_1722975600000.jpg
 */
@Component
public class MediaKeyGenerator {

    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-z0-9_]");
    private static final int     MAX_SLUG_LEN = 40;

    /**
     * Generate an S3 key for an uploaded file (no title slug).
     */
    public String generate(MediaModule module, Long communityId, String moduleId,
                           String subContext, MediaType mediaType, String extension) {
        return generate(module, communityId, moduleId, subContext, mediaType, extension, null);
    }

    /**
     * Generate an S3 key for an uploaded file, inserting a human-readable slug
     * derived from {@code title} (Media Title / Caption) between {@code moduleId}
     * and {@code subContext}.
     *
     * @param title  optional caption / title — slugified and inserted into the path.
     *               {@code null} or blank values are silently ignored.
     */
    public String generate(MediaModule module, Long communityId, String moduleId,
                           String subContext, MediaType mediaType, String extension,
                           String title) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ts   = String.valueOf(Instant.now().toEpochMilli());
        String base = resolveFolder(module, communityId, moduleId, subContext, mediaType, slugify(title));
        return base + "/" + uuid + "_" + ts + "." + extension;
    }

    /**
     * Generate the thumbnail variant key from an existing original key.
     * Replaces the "/original/" segment with "/thumbnail/".
     */
    public String thumbnailKey(String originalKey) {
        return originalKey.replace("/original/", "/thumbnail/");
    }

    /**
     * Generate the medium (1920px) variant key from an existing original key.
     */
    public String mediumKey(String originalKey) {
        return originalKey.replace("/original/", "/medium/");
    }

    /**
     * Generate the compressed (720p video or WebP image) variant key.
     */
    public String compressedKey(String originalKey) {
        return originalKey.replace("/original/", "/compressed/");
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String resolveFolder(MediaModule module, Long communityId,
                                  String moduleId, String subContext,
                                  MediaType mediaType, String titleSlug) {
        String ctx = (subContext != null && !subContext.isBlank())
                ? subContext
                : defaultContext(mediaType);

        // Insert title slug segment between moduleId and subContext (only when present)
        String slugSegment = (titleSlug != null && !titleSlug.isBlank()) ? titleSlug + "/" : "";

        return switch (module) {
            case EVENT       -> "events/"        + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case USER        -> "users/"         + moduleId    + "/" + slugSegment + ctx + "/original";
            case MARKETPLACE -> "marketplace/"   + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case SPORTS      -> "sports/"        + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case HEALTHCARE  -> "healthcare/"    + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case COMMUNITY   -> "communities/"   + communityId + "/" + slugSegment + ctx + "/original";
            case VISITOR     -> "visitor/"       + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case FINANCE     -> "finance/"       + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case COMPLAINT   -> "complaints/"    + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case ANNOUNCEMENT-> "announcements/" + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
            case DOCUMENT    -> "documents/"     + communityId + "/" + moduleId + "/" + slugSegment + ctx + "/original";
        };
    }

    /**
     * Converts a free-form title/caption into a safe S3 path segment.
     * e.g. "Visarjan Procession Evening!" → "visarjan_procession_evening"
     *
     * Rules:
     *  - Lowercase
     *  - Spaces and hyphens become underscores
     *  - Any character outside [a-z0-9_] is stripped
     *  - Trimmed to {@value MAX_SLUG_LEN} characters
     *  - Returns {@code null} if the result is blank (so the segment is omitted)
     */
    private String slugify(String title) {
        if (title == null || title.isBlank()) return null;
        String slug = title.trim()
                .toLowerCase()
                .replace('-', '_')
                .replace(' ', '_');
        slug = UNSAFE_CHARS.matcher(slug).replaceAll("");
        // Collapse consecutive underscores
        slug = slug.replaceAll("_+", "_").replaceAll("^_|_$", "");
        if (slug.isBlank()) return null;
        return slug.length() > MAX_SLUG_LEN ? slug.substring(0, MAX_SLUG_LEN) : slug;
    }

    private String defaultContext(MediaType type) {
        return switch (type) {
            case VIDEO       -> "videos";
            case DOCUMENT    -> "documents";
            case CERTIFICATE -> "certificates";
            case AUDIO       -> "audio";
            case QR_CODE     -> "qr";
            default          -> "gallery";
        };
    }
}
