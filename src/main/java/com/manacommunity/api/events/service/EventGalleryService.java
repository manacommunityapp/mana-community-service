package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventGalleryItemRequest;
import com.manacommunity.api.events.dto.EventGalleryItemResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventGalleryItem;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventGalleryItemRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.media.entity.MediaObject;
import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.media.service.MediaUrlService;
import com.manacommunity.api.media.service.S3MediaGateway;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventGalleryService {

    private final EventGalleryItemRepository galleryRepo;
    private final EventCommunityRepository eventRepo;

    /**
     * MediaRepository + MediaUrlService allow us to generate fresh
     * presigned / CloudFront URLs at read time from the stored S3 key,
     * preventing "Request has expired" (HTTP 403) errors after the
     * presigned URL TTL (3600 s) elapses.
     */
    private final MediaRepository mediaRepo;
    private final MediaUrlService urlService;
    private final S3MediaGateway s3Gateway;

    @Transactional(readOnly = true)
    public List<EventGalleryItemResponse> getByEvent(Long eventId, String albumName) {
        if (albumName != null && !albumName.isBlank()) {
            return galleryRepo.findByEventIdAndAlbumNameOrderBySortOrderAsc(eventId, albumName)
                    .stream().map(this::toResponse).toList();
        }
        return galleryRepo.findByEventIdOrderBySortOrderAscCreatedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<String> getAlbums(Long eventId) {
        return galleryRepo.findDistinctAlbumsByEvent(eventId);
    }

    @Transactional(readOnly = true)
    public List<EventGalleryItemResponse> getByCommunity(Long communityId, String dayTag, String category) {
        String dt = (dayTag != null && !dayTag.isBlank()) ? dayTag : null;
        String cat = (category != null && !category.isBlank()) ? category : null;
        return galleryRepo.findByCommunity(communityId, dt, cat)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventGalleryItemResponse create(EventGalleryItemRequest req, AppUser user, Community community) {
        EventCommunity event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        // Resolve MediaObject UUID and verify it was successfully uploaded to S3
        final UUID mediaExternalId = parseAndVerifyMediaId(req.getMediaId(), "gallery create");

        EventGalleryItem item = EventGalleryItem.builder()
                .event(event)
                .url(req.getUrl())
                .mediaExternalId(mediaExternalId)
                .thumbnailUrl(req.getThumbnailUrl())
                .mediaType(parseEnumOrDefault(EventGalleryItem.MediaType.class, req.getMediaType(), EventGalleryItem.MediaType.PHOTO))
                .albumName(req.getAlbumName())
                .dayTag(req.getDayTag())
                .category(req.getCategory())
                .caption(req.getCaption())
                .featured(req.isFeatured())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .community(community)
                .uploadedBy(user)
                .build();
        return toResponse(galleryRepo.save(item));
    }

    @Transactional
    public EventGalleryItemResponse update(Long id, EventGalleryItemRequest req) {
        EventGalleryItem item = galleryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery item", id));
        item.setUrl(req.getUrl());
        item.setThumbnailUrl(req.getThumbnailUrl());
        item.setMediaType(parseEnumOrDefault(EventGalleryItem.MediaType.class, req.getMediaType(), item.getMediaType()));
        item.setAlbumName(req.getAlbumName());
        item.setDayTag(req.getDayTag());
        item.setCategory(req.getCategory());
        item.setCaption(req.getCaption());
        item.setFeatured(req.isFeatured());
        item.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : item.getSortOrder());

        // Update mediaExternalId if a new one is supplied — verify S3 upload before committing
        if (req.getMediaId() != null && !req.getMediaId().isBlank()) {
            UUID newMediaId = parseAndVerifyMediaId(req.getMediaId(), "gallery update");
            if (newMediaId != null) item.setMediaExternalId(newMediaId);
        }

        return toResponse(galleryRepo.save(item));
    }

    @Transactional
    public void delete(Long id) {
        EventGalleryItem item = galleryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gallery item", id));

        if (item.getMediaExternalId() != null) {
            try {
                mediaRepo.findByExternalIdAndDeletedFalse(item.getMediaExternalId()).ifPresent(media -> {
                    // 1. Soft-delete the DB record
                    media.setDeleted(true);
                    media.setStatus(com.manacommunity.api.media.entity.MediaStatus.DELETED);
                    media.setDeletedAt(java.time.OffsetDateTime.now());
                    mediaRepo.save(media);

                    // 2. Physically delete all variants from S3
                    deleteFromS3(media);
                });
            } catch (Exception ex) {
                log.warn("Failed to delete linked MediaObject {} for gallery item {}: {}",
                        item.getMediaExternalId(), id, ex.getMessage());
            }
        }

        galleryRepo.delete(item);
    }

    // ── S3 physical delete helpers ────────────────────────────────────────────

    /**
     * Deletes all S3 keys associated with a MediaObject (original, thumbnail, compressed, medium).
     * Failures are logged but do NOT propagate — the DB soft-delete is already committed.
     */
    private void deleteFromS3(MediaObject media) {
        deleteS3Key(media.getBucketName(), media.getS3Key(), "original");
        deleteS3Key(media.getBucketName(), media.getThumbnailKey(), "thumbnail");
        deleteS3Key(media.getBucketName(), media.getCompressedKey(), "compressed");
        deleteS3Key(media.getBucketName(), media.getMediumKey(), "medium");
    }

    private void deleteS3Key(String bucket, String key, String variant) {
        if (bucket == null || key == null || key.isBlank()) return;
        try {
            s3Gateway.deleteObject(bucket, key);
            log.info("S3 {} deleted: bucket={} key={}", variant, bucket, key);
        } catch (Exception ex) {
            log.warn("Failed to delete S3 {} key={}: {}", variant, key, ex.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UUID parseAndVerifyMediaId(String mediaId, String context) {
        if (mediaId == null || mediaId.isBlank()) return null;
        UUID uuid;
        try {
            uuid = UUID.fromString(mediaId);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid mediaId UUID supplied to {}: {}", context, mediaId);
            return null;
        }
        mediaRepo.findByExternalIdAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("MediaObject", "id", uuid.toString()));
        return uuid;
    }

    /**
     * Maps a gallery item to its response DTO.
     *
     * <p>URL strategy (in priority order):
     * <ol>
     *   <li>If {@code mediaExternalId} is set → look up the {@link MediaObject} and
     *       use {@link MediaUrlService} to generate a <em>fresh</em> presigned /
     *       CloudFront URL. This prevents "Request has expired" (HTTP 403) errors.</li>
     *   <li>Otherwise fall back to the raw {@code url} stored in the gallery item
     *       (legacy items or URL-pasted entries).</li>
     * </ol>
     */
    private EventGalleryItemResponse toResponse(EventGalleryItem g) {
        String url         = g.getUrl();
        String thumbnailUrl = g.getThumbnailUrl();
        String compressedUrl = null;
        String mediumUrl    = null;
        String mediaId      = null;

        if (g.getMediaExternalId() != null) {
            Optional<MediaObject> mediaOpt = mediaRepo.findByExternalIdAndDeletedFalse(g.getMediaExternalId());
            if (mediaOpt.isPresent()) {
                MediaObject media = mediaOpt.get();
                // Generate fresh time-limited URLs from stored S3 keys
                String freshUrl = urlService.generateUrl(media);
                if (freshUrl != null) url = freshUrl;

                String freshThumb = urlService.generateThumbnailUrl(media);
                thumbnailUrl = (freshThumb != null) ? freshThumb : freshUrl;

                compressedUrl = urlService.generateCompressedUrl(media);
                mediumUrl     = urlService.generateMediumUrl(media);
                mediaId       = media.getExternalId().toString();
            } else {
                log.warn("MediaObject {} linked to gallery item {} not found or deleted; using stored URL",
                        g.getMediaExternalId(), g.getId());
            }
        }

        return EventGalleryItemResponse.builder()
                .id(g.getId())
                .eventId(g.getEvent().getId())
                .url(url)
                .thumbnailUrl(thumbnailUrl)
                .compressedUrl(compressedUrl)
                .mediumUrl(mediumUrl)
                .mediaType(g.getMediaType().name())
                .albumName(g.getAlbumName())
                .dayTag(g.getDayTag())
                .category(g.getCategory())
                .caption(g.getCaption())
                .featured(g.isFeatured())
                .sortOrder(g.getSortOrder())
                .uploadedByName(g.getUploadedBy().getFullName())
                .createdAt(g.getCreatedAt() != null ? g.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .mediaId(mediaId)
                .build();
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return def; }
    }
}
