package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventAuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventAuctionItemRepository extends JpaRepository<EventAuctionItem, Long> {

    @Modifying
    void deleteByEventId(Long eventId);

    @Modifying
    @Query("DELETE FROM EventAuctionBid b WHERE b.event.id = :eventId")
    void deleteAuctionBidsByEventId(@Param("eventId") Long eventId);

    List<EventAuctionItem> findByCommunityIdOrderBySortOrderAscIdAsc(Long communityId);

    List<EventAuctionItem> findByCommunityIdAndEventIdOrderBySortOrderAscIdAsc(Long communityId, Long eventId);

    Optional<EventAuctionItem> findByIdAndCommunityId(Long id, Long communityId);

    @Query("SELECT COALESCE(SUM(a.currentBid), 0) FROM EventAuctionItem a WHERE (:communityId IS NULL OR a.community.id = :communityId) AND (a.bidCount > 0 OR a.status = com.manacommunity.api.events.entity.EventAuctionItem.ItemStatus.CLOSED)")
    double sumCurrentBidsByCommunity(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(a) FROM EventAuctionItem a WHERE (:communityId IS NULL OR a.community.id = :communityId) AND (a.bidCount > 0 OR a.status = com.manacommunity.api.events.entity.EventAuctionItem.ItemStatus.CLOSED)")
    long countSoldOrBidItemsByCommunity(@Param("communityId") Long communityId);

    long countByCommunityIdAndBidCountGreaterThan(Long communityId, int minBids);

    long countByCommunityId(Long communityId);
}
