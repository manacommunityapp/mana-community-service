package com.manacommunity.api.repository;

import com.manacommunity.api.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * If a PostRepository already exists in the project, add these method
 * declarations to the existing interface instead of creating a new file.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    long countByCommunityId(Long communityId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.community.id = :communityId AND p.createdAt >= :after")
    long countByCommunityIdAndCreatedAtAfter(@Param("communityId") Long communityId,
                                             @Param("after") LocalDateTime after);
}
