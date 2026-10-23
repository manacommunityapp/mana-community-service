package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketProductRequestRepository extends JpaRepository<MarketProductRequest, Long> {
    Page<MarketProductRequest> findByCommunityIdAndStatus(Long communityId, MarketProductRequest.RequestStatus status, Pageable pageable);
    List<MarketProductRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}
