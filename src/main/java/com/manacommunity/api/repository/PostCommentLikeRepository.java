package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCommentLikeRepository extends JpaRepository<PostCommentLike, Long> {

    Optional<PostCommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    long countByCommentId(Long commentId);

    List<PostCommentLike> findByCommentIdOrderByCreatedAtDesc(Long commentId);

    void deleteByCommentIdAndUserId(Long commentId, Long userId);
}
