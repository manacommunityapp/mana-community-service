package com.cpn.domain.feed.repository;

import com.cpn.domain.feed.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByPostTypeOrderByCreatedAtDesc(String postType);
    List<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
}
