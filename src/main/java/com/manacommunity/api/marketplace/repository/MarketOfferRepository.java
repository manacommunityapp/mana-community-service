package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketOfferRepository extends JpaRepository<MarketOffer, Long> {
    List<MarketOffer> findByListingIdOrderByCreatedAtDesc(Long listingId);
    List<MarketOffer> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    List<MarketOffer> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    int countBySellerId(Long sellerId);
}
