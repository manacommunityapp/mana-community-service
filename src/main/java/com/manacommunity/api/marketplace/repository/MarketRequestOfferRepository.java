package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketRequestOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketRequestOfferRepository extends JpaRepository<MarketRequestOffer, Long> {
    List<MarketRequestOffer> findByProductRequestIdOrderByCreatedAtDesc(Long requestId);
    List<MarketRequestOffer> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
}
