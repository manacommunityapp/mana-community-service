package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketLostAndFound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketLostAndFoundRepository extends JpaRepository<MarketLostAndFound, Long> {
    Page<MarketLostAndFound> findByCommunityIdAndStatus(Long communityId, MarketLostAndFound.LostFoundStatus status, Pageable pageable);
    Page<MarketLostAndFound> findByCommunityIdAndTypeAndStatus(Long communityId, MarketLostAndFound.PostType type, MarketLostAndFound.LostFoundStatus status, Pageable pageable);
    List<MarketLostAndFound> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
}
