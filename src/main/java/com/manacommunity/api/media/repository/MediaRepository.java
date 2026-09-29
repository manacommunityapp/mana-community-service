package com.manacommunity.api.media.repository;

import com.manacommunity.api.media.entity.MediaModule;
import com.manacommunity.api.media.entity.MediaObject;
import com.manacommunity.api.media.entity.MediaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<MediaObject, Long> {

    Optional<MediaObject> findByExternalIdAndDeletedFalse(UUID externalId);

    Page<MediaObject> findByModuleAndModuleIdAndDeletedFalseOrderByUploadedAtDesc(
            MediaModule module, String moduleId, Pageable pageable);

    List<MediaObject> findByModuleAndModuleIdAndDeletedFalseAndStatusOrderBySortOrderAscUploadedAtDesc(
            MediaModule module, String moduleId, MediaStatus status);

    List<MediaObject> findByModuleAndModuleIdAndSubContextAndDeletedFalseAndStatus(
            MediaModule module, String moduleId, String subContext, MediaStatus status);

    Page<MediaObject> findByCommunityIdAndDeletedFalseAndStatus(
            Long communityId, MediaStatus status, Pageable pageable);

    /** Fetch the featured image for an event/module context. */
    Optional<MediaObject> findFirstByModuleAndModuleIdAndFeaturedTrueAndDeletedFalseAndStatus(
            MediaModule module, String moduleId, MediaStatus status);

    @Modifying
    @Query("""
            UPDATE MediaObject m
            SET m.thumbnailKey = :thumbKey, m.processingStatus = 'THUMBNAIL_GENERATED', m.updatedAt = CURRENT_TIMESTAMP
            WHERE m.id = :id
            """)
    void updateThumbnailKey(@Param("id") Long id, @Param("thumbKey") String thumbKey);

    @Modifying
    @Query("""
            UPDATE MediaObject m
            SET m.compressedKey = :compKey, m.mediumKey = :medKey,
                m.processingStatus = 'COMPLETE', m.updatedAt = CURRENT_TIMESTAMP
            WHERE m.id = :id
            """)
    void updateProcessingKeys(@Param("id") Long id,
                              @Param("compKey") String compKey,
                              @Param("medKey") String medKey);

    boolean existsByS3Key(String s3Key);
}
