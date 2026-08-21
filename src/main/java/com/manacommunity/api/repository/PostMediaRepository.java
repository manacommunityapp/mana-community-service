package com.manacommunity.api.repository;

import com.manacommunity.api.model.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findByPostIdOrderBySortOrderAsc(Long postId);
    void deleteByPostId(Long postId);

    @Query("SELECT pm FROM PostMedia pm WHERE pm.post.id IN :postIds ORDER BY pm.sortOrder ASC")
    List<PostMedia> findByPostIdInOrderBySortOrderAsc(@Param("postIds") Collection<Long> postIds);
}
