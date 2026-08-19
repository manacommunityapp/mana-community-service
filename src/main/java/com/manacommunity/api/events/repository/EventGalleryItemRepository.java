package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventGalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;

public interface EventGalleryItemRepository extends JpaRepository<EventGalleryItem, Long> {

    @Modifying
    void deleteByEventId(Long eventId);


    List<EventGalleryItem> findByEventIdOrderBySortOrderAscCreatedAtDesc(Long eventId);

    List<EventGalleryItem> findByEventIdAndAlbumNameOrderBySortOrderAsc(Long eventId, String albumName);

    @Query("SELECT DISTINCT g.albumName FROM EventGalleryItem g WHERE g.event.id = :eventId AND g.albumName IS NOT NULL ORDER BY g.albumName")
    List<String> findDistinctAlbumsByEvent(@Param("eventId") Long eventId);

    long countByEventId(Long eventId);

    @Query("SELECT g FROM EventGalleryItem g WHERE g.community.id = :communityId " +
           "AND (:dayTag IS NULL OR g.dayTag = :dayTag) " +
           "AND (:category IS NULL OR g.category = :category) " +
           "ORDER BY g.sortOrder ASC, g.createdAt DESC")
    List<EventGalleryItem> findByCommunity(
            @Param("communityId") Long communityId,
            @Param("dayTag") String dayTag,
            @Param("category") String category);
}
