package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventAuctionBidRequest;
import com.manacommunity.api.events.dto.EventAuctionBidResponse;
import com.manacommunity.api.events.dto.EventAuctionItemRequest;
import com.manacommunity.api.events.dto.EventAuctionItemResponse;
import com.manacommunity.api.events.dto.EventAuctionStatsResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;

import java.math.BigDecimal;
import java.util.List;

public interface EventAuctionService {

    List<EventAuctionItemResponse> getItems(Long communityId, Long eventId);

    EventAuctionItemResponse getItem(Long id, Long communityId);

    EventAuctionItemResponse createItem(EventAuctionItemRequest req, AppUser user, Community community);

    EventAuctionItemResponse updateItem(Long id, EventAuctionItemRequest req, Long communityId);

    void deleteItem(Long id, Long communityId);

    EventAuctionItemResponse placeBid(Long itemId, EventAuctionBidRequest bidReq, AppUser user, Community community);

    /**
     * Variant used by the STOMP WebSocket controller where only the
     * authenticated user's ID (from the STOMP principal) is available.
     * Looks up the AppUser and Community internally.
     *
     * @param itemId      the auction item to bid on
     * @param amount      bid amount
     * @param userId      authenticated bidder's user ID
     * @return the updated auction item response
     */
    EventAuctionItemResponse placeBidByUserId(Long itemId, BigDecimal amount, Long userId);

    List<EventAuctionBidResponse> getBids(Long itemId, Long communityId);

    List<EventAuctionBidResponse> getRecentBids(Long communityId);

    EventAuctionStatsResponse getStats(Long communityId, Long eventId);
}
