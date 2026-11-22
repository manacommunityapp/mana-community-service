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

    List<MediaObject> findByExternalIdInAndDeletedFalse(java.util.Collection<UUID> externalIds);

    /** Includes soft-deleted rows; used by restore. */
    Optional<MediaObject> findByExternalId(UUID externalId);

    Page<MediaObject> findByModuleAndModuleIdAndDeletedFalseOrderByUploadedAtDesc(
            MediaModule module, String moduleId, Pageable pageable);

    List<MediaObject> findByModuleAndModuleIdAndDeletedFalseAndStatusOrderBySortOrderAscUploadedAtDesc(
            MediaModule module, String moduleId, MediaStatus status);

    List<MediaObject> findByModuleAndModuleIdAndSubContextAndDeletedFalseAndStatus(
            MediaModule module, String moduleId, String subContext, MediaStatus status);

    // ── Community-scoped variants (multi-tenancy enforcement) ────────────────

    Optional<MediaObject> findByExternalIdAndCommunityIdAndDeletedFalse(
            UUID externalId, Long communityId);

    /** Includes soft-deleted rows; used by restore. */
    Optional<MediaObject> findByExternalIdAndCommunityId(UUID externalId, Long communityId);

    Page<MediaObject> findByModuleAndModuleIdAndCommunityIdAndDeletedFalseOrderByUploadedAtDesc(
            MediaModule module, String moduleId, Long communityId, Pageable pageable);

    List<MediaObject> findByModuleAndModuleIdAndCommunityIdAndDeletedFalseAndStatusOrderBySortOrderAscUploadedAtDesc(
            MediaModule module, String moduleId, Long communityId, MediaStatus status);

    List<MediaObject> findByModuleAndModuleIdAndCommunityIdAndSubContextAndDeletedFalseAndStatus(
            MediaModule module, String moduleId, Long communityId, String subContext, MediaStatus status);

    Page<MediaObject> findByCommunityIdAndDeletedFalseAndStatus(
            Long communityId, MediaStatus status, Pageable pageable);

    /** Fetch the featured image for an event/module context. */
    Optional<MediaObject> findFirstByModuleAndModuleIdAndFeaturedTrueAndDeletedFalseAndStatus(
            MediaModule module, String moduleId, MediaStatus status);

    @Modifying
    @Query(value = """
            UPDATE manacommunity.media_objects
            SET thumbnail_key = :thumbKey, processing_status = 'THUMBNAIL_GENERATED', updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, nativeQuery = true)
    void updateThumbnailKey(@Param("id") Long id, @Param("thumbKey") String thumbKey);

    @Modifying
    @Query(value = """
            UPDATE manacommunity.media_objects
            SET compressed_key = :compKey, medium_key = :medKey,
                processing_status = 'COMPLETE', updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, nativeQuery = true)
    void updateProcessingKeys(@Param("id") Long id,
                              @Param("compKey") String compKey,
                              @Param("medKey") String medKey);

    boolean existsByS3Key(String s3Key);

    /**
     * Owning community/communities of whichever media row holds {@code key} as any of its
     * four S3 keys (original, thumbnail, compressed, medium).
     *
     * <p>Used to tenant-scope the wildcard file-streaming path, which receives a bare S3 key
     * and — for keys outside the {@code users/{userId}/...} shape — has no other way to find
     * out who owns the object. {@code media_objects.community_id} is NOT NULL, so a hit always
     * yields a usable tenant; soft-deleted rows are included on purpose, because a soft-deleted
     * object's bytes are still in the bucket and must not become readable by other tenants.
     *
     * <p>Returns a list rather than an Optional: only {@code s3_key} is unique, so a key could
     * in principle appear as a variant key on more than one row.
     */
    @Query("""
            SELECT DISTINCT m.communityId FROM MediaObject m
             WHERE m.s3Key = :key
                OR m.thumbnailKey = :key
                OR m.compressedKey = :key
                OR m.mediumKey = :key
            """)
    List<Long> findCommunityIdsByAnyKey(@Param("key") String key);
}
