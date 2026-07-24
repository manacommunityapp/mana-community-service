package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {
    Optional<PostBookmark> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    Page<PostBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
