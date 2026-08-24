package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventAuctionBidRequest;
import com.manacommunity.api.events.dto.EventAuctionBidResponse;
import com.manacommunity.api.events.dto.EventAuctionItemRequest;
import com.manacommunity.api.events.dto.EventAuctionItemResponse;
import com.manacommunity.api.events.dto.EventAuctionStatsResponse;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface EventAuctionService {

    List<EventAuctionItemResponse> getItems(Long communityId, Long eventId);

    EventAuctionItemResponse getItem(Long id, Long communityId);

    EventAuctionItemResponse createItem(EventAuctionItemRequest req, AppUser user, Community community);

    EventAuctionItemResponse updateItem(Long id, EventAuctionItemRequest req, Long communityId);

    void deleteItem(Long id, Long communityId);

    EventAuctionItemResponse placeBid(Long itemId, EventAuctionBidRequest bidReq, AppUser user, Community community);

    List<EventAuctionBidResponse> getBids(Long itemId, Long communityId);

    List<EventAuctionBidResponse> getRecentBids(Long communityId);

    EventAuctionStatsResponse getStats(Long communityId, Long eventId);
}
