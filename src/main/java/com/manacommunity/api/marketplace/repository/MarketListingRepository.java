package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketListingRepository extends JpaRepository<MarketListing, Long> {

    Page<MarketListing> findByCommunityIdAndStatus(Long communityId, MarketListing.ListingStatus status, Pageable pageable);

    Page<MarketListing> findByCommunityIdAndCategoryAndStatus(Long communityId, String category, MarketListing.ListingStatus status, Pageable pageable);

    @Query("SELECT l FROM MarketListing l WHERE l.community.id = :communityId AND l.status = 'ACTIVE' AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MarketListing> searchActiveListings(@Param("communityId") Long communityId, @Param("search") String search, Pageable pageable);

    List<MarketListing> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<MarketListing> findBySellerIdAndStatus(Long sellerId, MarketListing.ListingStatus status);

    int countBySellerIdAndStatus(Long sellerId, MarketListing.ListingStatus status);

    int countByCommunityIdAndStatus(Long communityId, MarketListing.ListingStatus status);

    @Query("SELECT l.category, COUNT(l) FROM MarketListing l WHERE l.community.id = :communityId AND l.status = 'ACTIVE' GROUP BY l.category")
    List<Object[]> countListingsByCategory(@Param("communityId") Long communityId);
}
