package com.manacommunity.api.marketplace.repository;

import com.manacommunity.api.marketplace.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Page<Listing> findByCommunityIdAndStatus(Long communityId, Listing.ListingStatus status, Pageable pageable);

    Page<Listing> findByCommunityIdAndCategoryAndStatus(Long communityId, String category, Listing.ListingStatus status, Pageable pageable);

    List<Listing> findBySellerIdAndStatusNotOrderByCreatedAtDesc(Long sellerId, Listing.ListingStatus excludeStatus);

    @Query("SELECT l FROM Listing l WHERE l.community.id = :communityId AND l.status = :status " +
           "AND (LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Listing> searchByCommunity(@Param("communityId") Long communityId,
                                    @Param("query") String query,
                                    @Param("status") Listing.ListingStatus status,
                                    Pageable pageable);
}
