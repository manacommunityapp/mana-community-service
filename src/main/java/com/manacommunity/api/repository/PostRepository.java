package com.manacommunity.api.repository;

import com.manacommunity.api.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);
}
