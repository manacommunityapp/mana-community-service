package com.manacommunity.api.service;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.model.AuctionBid;
import com.manacommunity.api.model.AuctionConfig;
import com.manacommunity.api.model.AuctionPlayer;
import com.manacommunity.api.model.AuctionTeam;

import java.util.List;
import java.util.Optional;

public interface AuctionService {
    
    List<AuctionConfig> getConfigsBySportAndCommunity(Long sportId, Long communityId);
    List<AuctionConfig> getAllConfigsByCommunity(Long communityId);
    AuctionConfigResponse getConfigResponse(Long id);
    List<AuctionConfigResponse> getConfigResponsesBySportAndCommunity(Long sportId, Long communityId);
    List<AuctionConfigResponse> getConfigResponsesByCommunity(Long communityId);
    AuctionConfig createConfig(AuctionConfigRequest req, Long adminUserId);
    AuctionConfig updateConfig(Long configId, AuctionConfigRequest req);
    AuctionConfig updateStatus(Long configId, String status);
    
    // Live Auction actions
    AuctionBid placeBid(BidRequest req, Long biddingUserId);
    AuctionPlayer soldPlayer(SoldPlayerRequest req, Long adminUserId);
    AuctionPlayer passPlayer(Long playerId, Long adminUserId);
    // History and Lists
    AuctionStatsResponse getAuctionStats(Long configId);
    long getConfirmedRegistrationCount(Long configId);
    List<AuctionBid> getBidHistory(Long playerId);
    List<AuctionPlayer> getPlayers(Long configId, String category, String status);

    PlayerWithBidResponse getCurrentPlayer(Long configId);
    PlayerWithBidResponse pickRandomPlayer(Long configId);
    
    AuctionPlayer createPlayer(Long configId, AuctionPlayerRequest req);
}
