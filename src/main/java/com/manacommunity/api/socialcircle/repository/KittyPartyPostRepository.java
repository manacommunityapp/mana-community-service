package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KittyPartyPostRepository extends JpaRepository<KittyPartyPost, Long> {

    @Query("SELECT p FROM KittyPartyPost p WHERE p.circle.id = :circleId AND p.status <> 'DELETED' ORDER BY p.isPinned DESC, p.createdAt DESC")
    List<KittyPartyPost> findByCircleIdOrderByPinnedAndCreatedAt(@Param("circleId") Long circleId);

    List<KittyPartyPost> findByCircleIdAndPostType(Long circleId, String postType);

    List<KittyPartyPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
