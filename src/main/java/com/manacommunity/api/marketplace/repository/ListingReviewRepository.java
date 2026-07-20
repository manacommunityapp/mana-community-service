package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.ListingReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ListingReviewRepository extends JpaRepository<ListingReview, Long> {

    Page<ListingReview> findByListingIdOrderByCreatedAtDesc(Long listingId, Pageable pageable);

    Optional<ListingReview> findByListingIdAndReviewerId(Long listingId, Long reviewerId);

    boolean existsByListingIdAndReviewerId(Long listingId, Long reviewerId);

    @Query("SELECT AVG(r.rating) FROM ListingReview r WHERE r.listing.id = :listingId")
    Double findAverageRatingByListingId(@Param("listingId") Long listingId);

    @Query("SELECT COUNT(r) FROM ListingReview r WHERE r.listing.id = :listingId")
    long countByListingId(@Param("listingId") Long listingId);

    @Query("SELECT AVG(r.rating) FROM ListingReview r WHERE r.listing.seller.id = :sellerId")
    Double findAverageRatingBySellerId(@Param("sellerId") Long sellerId);
}
