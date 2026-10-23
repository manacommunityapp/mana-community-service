package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketListingImageRepository extends JpaRepository<MarketListingImage, Long> {
    List<MarketListingImage> findByListingIdOrderBySortOrderAsc(Long listingId);
    void deleteByListingId(Long listingId);
}
