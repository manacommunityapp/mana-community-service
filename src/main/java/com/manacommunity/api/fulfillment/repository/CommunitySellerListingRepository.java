package com.manacommunity.api.fulfillment.repository;

import com.manacommunity.api.fulfillment.entity.CommunitySellerListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunitySellerListingRepository extends JpaRepository<CommunitySellerListing, Long> {

    Page<CommunitySellerListing> findByCommunityIdAndIsAvailableTrue(Long communityId, Pageable pageable);

    Page<CommunitySellerListing> findBySellerId(Long sellerId, Pageable pageable);

    Page<CommunitySellerListing> findByCommunityIdAndCategoryAndIsAvailableTrue(
            Long communityId, String category, Pageable pageable);
}
