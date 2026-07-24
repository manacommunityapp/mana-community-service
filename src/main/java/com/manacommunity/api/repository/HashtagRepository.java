package com.manacommunity.api.repository;

import com.manacommunity.api.model.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByNameAndCommunityId(String name, Long communityId);

    @Query("SELECT h FROM Hashtag h WHERE h.community.id = :communityId ORDER BY h.postCount DESC")
    List<Hashtag> findTopHashtags(Long communityId, Pageable pageable);

    @Query("SELECT h FROM Hashtag h WHERE h.community.id = :communityId AND h.trending = true ORDER BY h.postCount DESC")
    List<Hashtag> findTrendingHashtags(Long communityId, Pageable pageable);

    @Query("SELECT h FROM Hashtag h WHERE h.community.id = :communityId AND LOWER(h.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Hashtag> searchHashtags(Long communityId, String query, Pageable pageable);
}
