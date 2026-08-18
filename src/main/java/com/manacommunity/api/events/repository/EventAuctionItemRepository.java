package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventAuctionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventAuctionItemRepository extends JpaRepository<EventAuctionItem, Long> {

    List<EventAuctionItem> findByCommunityIdOrderBySortOrderAsc(Long communityId);

    List<EventAuctionItem> findByEventId(Long eventId);

    @Query("SELECT COALESCE(SUM(a.currentBid), 0) FROM EventAuctionItem a WHERE a.community.id = :communityId AND a.bidCount > 0")
    double sumCurrentBidsByCommunity(@Param("communityId") Long communityId);

    long countByCommunityIdAndBidCountGreaterThan(Long communityId, int minBids);
}
