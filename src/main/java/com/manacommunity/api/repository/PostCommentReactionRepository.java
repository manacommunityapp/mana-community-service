package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostCommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCommentReactionRepository extends JpaRepository<PostCommentReaction, Long> {

    Optional<PostCommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    List<PostCommentReaction> findByCommentIdOrderByCreatedAtDesc(Long commentId);

    /** Returns rows of [reactionType (String), count (Long)] grouped by type */
    @Query("SELECT r.reactionType, COUNT(r) FROM PostCommentReaction r WHERE r.comment.id = :commentId GROUP BY r.reactionType")
    List<Object[]> countReactionsByCommentId(Long commentId);
}