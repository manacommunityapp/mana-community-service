package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findByPostIdOrderBySortOrderAsc(Long postId);
    void deleteByPostId(Long postId);
}
