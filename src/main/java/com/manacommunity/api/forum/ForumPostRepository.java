package com.manacommunity.api.forum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    Page<ForumPost> findByCommunityIdOrderByPinnedDescCreatedAtDesc(Long communityId, Pageable pageable);

    Page<ForumPost> findByCommunityIdAndCategoryOrderByCreatedAtDesc(Long communityId, String category, Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.community.id = :communityId " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<ForumPost> searchByCommunity(Long communityId, String q, Pageable pageable);

    List<ForumPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<ForumPost> findByCommunityIdAndPinnedTrueOrderByCreatedAtDesc(Long communityId);
}
