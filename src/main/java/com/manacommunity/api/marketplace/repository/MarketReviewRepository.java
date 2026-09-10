package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.MarketReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketReviewRepository extends JpaRepository<MarketReview, Long> {
    List<MarketReview> findByListingIdOrderByCreatedAtDesc(Long listingId);

    @Query("SELECT r FROM MarketReview r WHERE r.listing.seller.id = :sellerId ORDER BY r.createdAt DESC")
    List<MarketReview> findBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM MarketReview r WHERE r.listing.seller.id = :sellerId")
    Double calculateAverageRatingForSeller(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r) FROM MarketReview r WHERE r.listing.seller.id = :sellerId")
    int countBySellerId(@Param("sellerId") Long sellerId);
}
