package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    Page<PostComment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);
}
