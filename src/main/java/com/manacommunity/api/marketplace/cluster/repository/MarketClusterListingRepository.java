package com.manacommunity.api.marketplace.cluster.repository;

import com.manacommunity.api.marketplace.cluster.entity.MarketClusterListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MarketClusterListingRepository extends JpaRepository<MarketClusterListing, Long> {

    List<MarketClusterListing> findByCommunityIdAndModerationStatusAndExpiresAtAfter(
            Long communityId, String moderationStatus, LocalDateTime now);

    @Query("""
           SELECT l FROM MarketClusterListing l
           WHERE l.moderationStatus = 'ACTIVE'
             AND l.expiresAt > :now
             AND (l.community.id = :userCommunityId
                  OR (l.visibilityScope = 'CLUSTER_3KM' AND l.s2CellIdL13 IN :s2Cells))
           """)
    List<MarketClusterListing> findActiveHyperlocalCandidates(
            @Param("userCommunityId") Long userCommunityId,
            @Param("s2Cells") Collection<String> s2Cells,
            @Param("now") LocalDateTime now);

    long countBySeller_IdAndCreatedAtAfter(Long sellerId, LocalDateTime since);
}
