package com.manacommunity.api.repository;

import com.manacommunity.api.model.Post;
import com.manacommunity.api.model.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);
    Page<Post> findByCommunityIdAndPostTypeOrderByCreatedAtDesc(Long communityId, PostType postType, Pageable pageable);
    Page<Post> findByCommunityIdAndOfficialTrueOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<Post> findByCommunityIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(Long communityId, Pageable pageable);
    Page<Post> findByCommunityIdAndPostTypeAndDeletedFalseOrderByCreatedAtDesc(Long communityId, PostType postType, Pageable pageable);
    Page<Post> findByCommunityIdAndOfficialTrueAndDeletedFalseOrderByCreatedAtDesc(Long communityId, Pageable pageable);
    Page<Post> findByGroupIdAndDeletedFalseOrderByPinnedDescCreatedAtDesc(Long groupId, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN PostBookmark b ON b.post.id = p.id WHERE b.user.id = :userId AND p.deleted = false ORDER BY b.createdAt DESC")
    Page<Post> findBookmarkedByUser(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.community.id = :communityId AND p.deleted = false AND (LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.hashtags) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(Long communityId, String query, Pageable pageable);

    @Query("SELECT p.postType, COUNT(p) FROM Post p WHERE p.community.id = :communityId AND p.deleted = false GROUP BY p.postType")
    java.util.List<Object[]> countPostsByType(Long communityId);

    long countByCommunityIdAndDeletedFalse(Long communityId);
}
