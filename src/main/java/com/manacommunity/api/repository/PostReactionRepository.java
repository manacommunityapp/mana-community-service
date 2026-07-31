package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostReaction;
import com.manacommunity.api.model.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
    List<PostReaction> findByPostId(Long postId);

    @Query("SELECT r.reactionType, COUNT(r) FROM PostReaction r WHERE r.post.id = :postId GROUP BY r.reactionType")
    List<Object[]> countReactionsByType(Long postId);

    @Query("SELECT r.reactionType FROM PostReaction r WHERE r.post.id = :postId AND r.user.id = :userId")
    Optional<ReactionType> findReactionTypeByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);
}
