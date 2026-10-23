package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventAuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventAuctionBidRepository extends JpaRepository<EventAuctionBid, Long> {

    List<EventAuctionBid> findByItemIdOrderByBidAtDesc(Long itemId);

    List<EventAuctionBid> findByCommunityIdOrderByBidAtDesc(Long communityId);

    List<EventAuctionBid> findTop20ByCommunityIdOrderByBidAtDesc(Long communityId);

    long countByCommunityId(Long communityId);

    @Query("SELECT b.bidderName as name, SUM(b.amount) as total, COUNT(b) as count " +
           "FROM EventAuctionBid b WHERE b.community.id = :communityId " +
           "GROUP BY b.bidderName ORDER BY total DESC")
    List<Object[]> findLeaderboardByCommunity(@Param("communityId") Long communityId);
}
