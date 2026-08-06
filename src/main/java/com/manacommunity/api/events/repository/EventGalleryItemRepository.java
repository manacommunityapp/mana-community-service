package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventGalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventGalleryItemRepository extends JpaRepository<EventGalleryItem, Long> {

    List<EventGalleryItem> findByEventIdOrderBySortOrderAscCreatedAtDesc(Long eventId);

    List<EventGalleryItem> findByEventIdAndAlbumNameOrderBySortOrderAsc(Long eventId, String albumName);

    @Query("SELECT DISTINCT g.albumName FROM EventGalleryItem g WHERE g.event.id = :eventId AND g.albumName IS NOT NULL ORDER BY g.albumName")
    List<String> findDistinctAlbumsByEvent(@Param("eventId") Long eventId);

    long countByEventId(Long eventId);
}
