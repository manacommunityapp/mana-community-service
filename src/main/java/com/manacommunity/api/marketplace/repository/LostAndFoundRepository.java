package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.LostAndFound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LostAndFoundRepository extends JpaRepository<LostAndFound, Long> {

    Page<LostAndFound> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, LostAndFound.LostFoundStatus status, Pageable pageable);

    Page<LostAndFound> findByCommunityIdAndTypeAndStatusOrderByCreatedAtDesc(Long communityId, LostAndFound.PostType type, LostAndFound.LostFoundStatus status, Pageable pageable);

    List<LostAndFound> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
}
