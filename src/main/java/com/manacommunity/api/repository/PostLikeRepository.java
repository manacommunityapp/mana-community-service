package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    java.util.List<PostLike> findByPostIdOrderByCreatedAtDesc(Long postId);
    void deleteByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    Set<Long> findLikedPostIdsByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}
