package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventGalleryItemRequest;
import com.manacommunity.api.events.dto.EventGalleryItemResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventGalleryItem;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventGalleryItemRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventGalleryService {

    private final EventGalleryItemRepository galleryRepo;
    private final CommunityEventRepository eventRepo;

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
        CommunityEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + req.getEventId()));

        EventGalleryItem item = EventGalleryItem.builder()
                .event(event)
                .url(req.getUrl())
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
                .orElseThrow(() -> new IllegalArgumentException("Gallery item not found: " + id));
        item.setUrl(req.getUrl());
        item.setThumbnailUrl(req.getThumbnailUrl());
        item.setMediaType(parseEnumOrDefault(EventGalleryItem.MediaType.class, req.getMediaType(), item.getMediaType()));
        item.setAlbumName(req.getAlbumName());
        item.setDayTag(req.getDayTag());
        item.setCategory(req.getCategory());
        item.setCaption(req.getCaption());
        item.setFeatured(req.isFeatured());
        item.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : item.getSortOrder());
        return toResponse(galleryRepo.save(item));
    }

    @Transactional
    public void delete(Long id) {
        galleryRepo.deleteById(id);
    }

    private EventGalleryItemResponse toResponse(EventGalleryItem g) {
        return EventGalleryItemResponse.builder()
                .id(g.getId())
                .eventId(g.getEvent().getId())
                .url(g.getUrl())
                .thumbnailUrl(g.getThumbnailUrl())
                .mediaType(g.getMediaType().name())
                .albumName(g.getAlbumName())
                .dayTag(g.getDayTag())
                .category(g.getCategory())
                .caption(g.getCaption())
                .featured(g.isFeatured())
                .sortOrder(g.getSortOrder())
                .uploadedByName(g.getUploadedBy().getFullName())
                .createdAt(g.getCreatedAt() != null ? g.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return def; }
    }
}
