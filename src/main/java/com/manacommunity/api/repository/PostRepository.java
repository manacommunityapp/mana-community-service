package com.manacommunity.api.repository;

import com.manacommunity.api.dto.PostFeedProjection;
import com.manacommunity.api.model.Post;
import com.manacommunity.api.model.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = {"user", "group"})
    Page<Post> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    Page<Post> findByCommunityIdAndPostTypeOrderByCreatedAtDesc(Long communityId, PostType postType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    Page<Post> findByCommunityIdAndOfficialTrueOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    Page<Post> findByCommunityIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(Long communityId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    Page<Post> findByCommunityIdAndPostTypeAndDeletedFalseOrderByCreatedAtDesc(Long communityId, PostType postType, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    Page<Post> findByCommunityIdAndOfficialTrueAndDeletedFalseOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    Page<Post> findByGroupIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(Long groupId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    @Query("SELECT p FROM Post p JOIN PostBookmark b ON b.post.id = p.id WHERE b.user.id = :userId AND p.deleted = false ORDER BY b.createdAt DESC")
    Page<Post> findBookmarkedByUser(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "group"})
    @Query("SELECT p FROM Post p WHERE p.community.id = :communityId AND p.deleted = false AND (LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.hashtags) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(Long communityId, String query, Pageable pageable);

    @Query("""
        SELECT 
            p.id AS id,
            p.content AS content,
            p.title AS title,
            p.imageUrl AS imageUrl,
            p.official AS official,
            p.pinned AS pinned,
            p.likesCount AS likesCount,
            p.commentsCount AS commentsCount,
            p.sharesCount AS sharesCount,
            p.bookmarksCount AS bookmarksCount,
            p.viewsCount AS viewsCount,
            p.createdAt AS createdAt,
            p.postType AS postType,
            p.visibility AS visibility,
            p.priority AS priority,
            p.price AS price,
            p.location AS location,
            p.pollQuestion AS pollQuestion,
            p.pollOptions AS pollOptions,
            p.pollEndDate AS pollEndDate,
            p.pollAnonymous AS pollAnonymous,
            p.hashtags AS hashtags,
            p.mentions AS mentions,
            p.linkUrl AS linkUrl,
            p.linkTitle AS linkTitle,
            p.linkDescription AS linkDescription,
            p.linkImage AS linkImage,
            p.eventDate AS eventDate,
            p.eventEndDate AS eventEndDate,
            p.eventVenue AS eventVenue,
            p.moderationStatus AS moderationStatus,
            u.id AS authorId,
            u.fullName AS authorFullName,
            u.role AS authorRole,
            u.profilePicUrl AS authorProfilePicUrl,
            g.id AS groupId,
            g.name AS groupName,
            g.slug AS groupSlug,
            g.iconUrl AS groupIconUrl,
            g.groupType AS groupType
        FROM Post p
        LEFT JOIN p.user u
        LEFT JOIN p.group g
        WHERE p.community.id = :communityId AND p.deleted = false
        ORDER BY p.pinned DESC, p.createdAt DESC
    """)
    Page<PostFeedProjection> findFeedStreamByCommunityId(@Param("communityId") Long communityId, Pageable pageable);

    @Query("""
        SELECT 
            p.id AS id,
            p.content AS content,
            p.title AS title,
            p.imageUrl AS imageUrl,
            p.official AS official,
            p.pinned AS pinned,
            p.likesCount AS likesCount,
            p.commentsCount AS commentsCount,
            p.sharesCount AS sharesCount,
            p.bookmarksCount AS bookmarksCount,
            p.viewsCount AS viewsCount,
            p.createdAt AS createdAt,
            p.postType AS postType,
            p.visibility AS visibility,
            p.priority AS priority,
            p.price AS price,
            p.location AS location,
            p.pollQuestion AS pollQuestion,
            p.pollOptions AS pollOptions,
            p.pollEndDate AS pollEndDate,
            p.pollAnonymous AS pollAnonymous,
            p.hashtags AS hashtags,
            p.mentions AS mentions,
            p.linkUrl AS linkUrl,
            p.linkTitle AS linkTitle,
            p.linkDescription AS linkDescription,
            p.linkImage AS linkImage,
            p.eventDate AS eventDate,
            p.eventEndDate AS eventEndDate,
            p.eventVenue AS eventVenue,
            p.moderationStatus AS moderationStatus,
            u.id AS authorId,
            u.fullName AS authorFullName,
            u.role AS authorRole,
            u.profilePicUrl AS authorProfilePicUrl,
            g.id AS groupId,
            g.name AS groupName,
            g.slug AS groupSlug,
            g.iconUrl AS groupIconUrl,
            g.groupType AS groupType
        FROM Post p
        LEFT JOIN p.user u
        LEFT JOIN p.group g
        WHERE p.community.id = :communityId AND p.postType = :postType AND p.deleted = false
        ORDER BY p.createdAt DESC
    """)
    Page<PostFeedProjection> findFeedStreamByCommunityIdAndPostType(@Param("communityId") Long communityId, @Param("postType") PostType postType, Pageable pageable);

    @Query("""
        SELECT 
            p.id AS id,
            p.content AS content,
            p.title AS title,
            p.imageUrl AS imageUrl,
            p.official AS official,
            p.pinned AS pinned,
            p.likesCount AS likesCount,
            p.commentsCount AS commentsCount,
            p.sharesCount AS sharesCount,
            p.bookmarksCount AS bookmarksCount,
            p.viewsCount AS viewsCount,
            p.createdAt AS createdAt,
            p.postType AS postType,
            p.visibility AS visibility,
            p.priority AS priority,
            p.price AS price,
            p.location AS location,
            p.pollQuestion AS pollQuestion,
            p.pollOptions AS pollOptions,
            p.pollEndDate AS pollEndDate,
            p.pollAnonymous AS pollAnonymous,
            p.hashtags AS hashtags,
            p.mentions AS mentions,
            p.linkUrl AS linkUrl,
            p.linkTitle AS linkTitle,
            p.linkDescription AS linkDescription,
            p.linkImage AS linkImage,
            p.eventDate AS eventDate,
            p.eventEndDate AS eventEndDate,
            p.eventVenue AS eventVenue,
            p.moderationStatus AS moderationStatus,
            u.id AS authorId,
            u.fullName AS authorFullName,
            u.role AS authorRole,
            u.profilePicUrl AS authorProfilePicUrl,
            g.id AS groupId,
            g.name AS groupName,
            g.slug AS groupSlug,
            g.iconUrl AS groupIconUrl,
            g.groupType AS groupType
        FROM Post p
        LEFT JOIN p.user u
        LEFT JOIN p.group g
        WHERE p.community.id = :communityId AND p.official = true AND p.deleted = false
        ORDER BY p.createdAt DESC
    """)
    Page<PostFeedProjection> findFeedStreamByCommunityIdAndOfficialTrue(@Param("communityId") Long communityId, Pageable pageable);

    @Query("""
        SELECT 
            p.id AS id,
            p.content AS content,
            p.title AS title,
            p.imageUrl AS imageUrl,
            p.official AS official,
            p.pinned AS pinned,
            p.likesCount AS likesCount,
            p.commentsCount AS commentsCount,
            p.sharesCount AS sharesCount,
            p.bookmarksCount AS bookmarksCount,
            p.viewsCount AS viewsCount,
            p.createdAt AS createdAt,
            p.postType AS postType,
            p.visibility AS visibility,
            p.priority AS priority,
            p.price AS price,
            p.location AS location,
            p.pollQuestion AS pollQuestion,
            p.pollOptions AS pollOptions,
            p.pollEndDate AS pollEndDate,
            p.pollAnonymous AS pollAnonymous,
            p.hashtags AS hashtags,
            p.mentions AS mentions,
            p.linkUrl AS linkUrl,
            p.linkTitle AS linkTitle,
            p.linkDescription AS linkDescription,
            p.linkImage AS linkImage,
            p.eventDate AS eventDate,
            p.eventEndDate AS eventEndDate,
            p.eventVenue AS eventVenue,
            p.moderationStatus AS moderationStatus,
            u.id AS authorId,
            u.fullName AS authorFullName,
            u.role AS authorRole,
            u.profilePicUrl AS authorProfilePicUrl,
            g.id AS groupId,
            g.name AS groupName,
            g.slug AS groupSlug,
            g.iconUrl AS groupIconUrl,
            g.groupType AS groupType
        FROM Post p
        JOIN PostBookmark b ON b.post.id = p.id
        LEFT JOIN p.user u
        LEFT JOIN p.group g
        WHERE b.user.id = :userId AND p.deleted = false
        ORDER BY b.createdAt DESC
    """)
    Page<PostFeedProjection> findFeedStreamBookmarkedByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT 
            p.id AS id,
            p.content AS content,
            p.title AS title,
            p.imageUrl AS imageUrl,
            p.official AS official,
            p.pinned AS pinned,
            p.likesCount AS likesCount,
            p.commentsCount AS commentsCount,
            p.sharesCount AS sharesCount,
            p.bookmarksCount AS bookmarksCount,
            p.viewsCount AS viewsCount,
            p.createdAt AS createdAt,
            p.postType AS postType,
            p.visibility AS visibility,
            p.priority AS priority,
            p.price AS price,
            p.location AS location,
            p.pollQuestion AS pollQuestion,
            p.pollOptions AS pollOptions,
            p.pollEndDate AS pollEndDate,
            p.pollAnonymous AS pollAnonymous,
            p.hashtags AS hashtags,
            p.mentions AS mentions,
            p.linkUrl AS linkUrl,
            p.linkTitle AS linkTitle,
            p.linkDescription AS linkDescription,
            p.linkImage AS linkImage,
            p.eventDate AS eventDate,
            p.eventEndDate AS eventEndDate,
            p.eventVenue AS eventVenue,
            p.moderationStatus AS moderationStatus,
            u.id AS authorId,
            u.fullName AS authorFullName,
            u.role AS authorRole,
            u.profilePicUrl AS authorProfilePicUrl,
            g.id AS groupId,
            g.name AS groupName,
            g.slug AS groupSlug,
            g.iconUrl AS groupIconUrl,
            g.groupType AS groupType
        FROM Post p
        LEFT JOIN p.user u
        LEFT JOIN p.group g
        WHERE p.group.id = :groupId AND p.deleted = false
        ORDER BY p.pinned DESC, p.createdAt DESC
    """)
    Page<PostFeedProjection> findFeedStreamByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query("""
        SELECT 
            p.id AS id,
            p.content AS content,
            p.title AS title,
            p.imageUrl AS imageUrl,
            p.official AS official,
            p.pinned AS pinned,
            p.likesCount AS likesCount,
            p.commentsCount AS commentsCount,
            p.sharesCount AS sharesCount,
            p.bookmarksCount AS bookmarksCount,
            p.viewsCount AS viewsCount,
            p.createdAt AS createdAt,
            p.postType AS postType,
            p.visibility AS visibility,
            p.priority AS priority,
            p.price AS price,
            p.location AS location,
            p.pollQuestion AS pollQuestion,
            p.pollOptions AS pollOptions,
            p.pollEndDate AS pollEndDate,
            p.pollAnonymous AS pollAnonymous,
            p.hashtags AS hashtags,
            p.mentions AS mentions,
            p.linkUrl AS linkUrl,
            p.linkTitle AS linkTitle,
            p.linkDescription AS linkDescription,
            p.linkImage AS linkImage,
            p.eventDate AS eventDate,
            p.eventEndDate AS eventEndDate,
            p.eventVenue AS eventVenue,
            p.moderationStatus AS moderationStatus,
            u.id AS authorId,
            u.fullName AS authorFullName,
            u.role AS authorRole,
            u.profilePicUrl AS authorProfilePicUrl,
            g.id AS groupId,
            g.name AS groupName,
            g.slug AS groupSlug,
            g.iconUrl AS groupIconUrl,
            g.groupType AS groupType
        FROM Post p
        LEFT JOIN p.user u
        LEFT JOIN p.group g
        WHERE p.community.id = :communityId AND p.deleted = false AND (LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.hashtags) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY p.createdAt DESC
    """)
    Page<PostFeedProjection> searchFeedStream(@Param("communityId") Long communityId, @Param("query") String query, Pageable pageable);

    @Query("SELECT p.postType, COUNT(p) FROM Post p WHERE p.community.id = :communityId AND p.deleted = false GROUP BY p.postType")
    java.util.List<Object[]> countPostsByType(Long communityId);

    Page<Post> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndDeletedFalse(Long userId);

    long countByCommunityIdAndDeletedFalse(Long communityId);

    long countByCommunityIdAndOfficialTrueAndDeletedFalse(Long communityId);

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.community.id = :communityId AND p.createdAt >= :after")
    long countByCommunityIdAndCreatedAtAfter(@Param("communityId") Long communityId,
                                             @Param("after") LocalDateTime after);
}
