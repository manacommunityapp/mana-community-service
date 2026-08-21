package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {
    Optional<PostBookmark> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    Page<PostBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    void deleteByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT pb.post.id FROM PostBookmark pb WHERE pb.user.id = :userId AND pb.post.id IN :postIds")
    Set<Long> findBookmarkedPostIdsByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
