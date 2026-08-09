package com.manacommunity.api.media.service;

import com.manacommunity.api.media.config.MediaProperties;
import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.entity.MediaType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Generates deterministic, UUID-based S3 object keys.
 *
 * Rules:
 *  - NEVER use original file names in the S3 key.
 *  - Always include UUID + epoch millis for uniqueness.
 *  - Key structure reflects the module/context hierarchy for easy lifecycle management.
 *
 * Key pattern:
 *   {module}/{communityId}/{moduleId}/{subContext}/original/{uuid}_{epochMs}.{ext}
 *
 * Examples:
 *   events/1001/EVT100001/gallery/original/2f8d7f4a7fbc4d2d_1722975600000.webp
 *   users/5001/profile/original/a3f89c21dd4e4abc_1722975600000.jpg
 *   marketplace/1001/ITEM9001/images/original/b7e12d44cc8f4321_1722975600000.jpg
 */
@Component
public class MediaKeyGenerator {

    /**
     * Generate an S3 key for an uploaded file.
     *
     * @param module      module that owns the media
     * @param communityId community identifier
     * @param moduleId    entity identifier within the module
     * @param subContext  sub-folder within the module (gallery, banner, sponsor …)
     * @param mediaType   IMAGE | VIDEO | DOCUMENT | AUDIO
     * @param extension   file extension derived from MIME type
     * @return  unique S3 key string
     */
    public String generate(MediaModule module, Long communityId, String moduleId,
                           String subContext, MediaType mediaType, String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ts   = String.valueOf(Instant.now().toEpochMilli());
        String base = resolveFolder(module, communityId, moduleId, subContext, mediaType);
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
                                  String moduleId, String subContext, MediaType mediaType) {
        String ctx = (subContext != null && !subContext.isBlank())
                ? subContext
                : defaultContext(mediaType);

        return switch (module) {
            case EVENT       -> "events/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case USER        -> "users/" + moduleId + "/" + ctx + "/original";
            case MARKETPLACE -> "marketplace/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case SPORTS      -> "sports/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case HEALTHCARE  -> "healthcare/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case COMMUNITY   -> "communities/" + communityId + "/" + ctx + "/original";
            case VISITOR     -> "visitor/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case FINANCE     -> "finance/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case COMPLAINT   -> "complaints/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case ANNOUNCEMENT-> "announcements/" + communityId + "/" + moduleId + "/" + ctx + "/original";
            case DOCUMENT    -> "documents/" + communityId + "/" + moduleId + "/" + ctx + "/original";
        };
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
