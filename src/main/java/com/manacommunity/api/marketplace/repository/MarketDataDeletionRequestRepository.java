package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketDataDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketDataDeletionRequestRepository extends JpaRepository<MarketDataDeletionRequest, Long> {
    List<MarketDataDeletionRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<MarketDataDeletionRequest> findByStatus(MarketDataDeletionRequest.DeletionStatus status);
}
