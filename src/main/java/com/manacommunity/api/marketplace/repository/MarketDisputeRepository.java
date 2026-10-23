package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketDispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketDisputeRepository extends JpaRepository<MarketDispute, Long> {
    Page<MarketDispute> findByCommunityIdAndStatus(Long communityId, MarketDispute.DisputeStatus status, Pageable pageable);
    List<MarketDispute> findByRaisedByIdOrderByCreatedAtDesc(Long raisedById);
    int countByCommunityIdAndStatus(Long communityId, MarketDispute.DisputeStatus status);
}
