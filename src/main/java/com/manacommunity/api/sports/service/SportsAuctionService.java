package com.manacommunity.api.sports.service;
import com.manacommunity.api.sports.dto.PlayerWithBidResponse;
import com.manacommunity.api.sports.dto.SoldPlayerRequest;
import com.manacommunity.api.sports.dto.SportsAuctionConfigRequest;
import com.manacommunity.api.sports.dto.SportsAuctionConfigResponse;
import com.manacommunity.api.sports.dto.SportsAuctionPlayerRequest;
import com.manacommunity.api.sports.dto.SportsAuctionStatsResponse;


import com.manacommunity.api.dto.*;
import com.manacommunity.api.sports.model.SportsAuctionBid;
import com.manacommunity.api.sports.model.SportsAuctionConfig;
import com.manacommunity.api.sports.model.SportsAuctionPlayer;
import com.manacommunity.api.sports.model.SportsAuctionTeam;

import java.util.List;
import java.util.Optional;

public interface SportsAuctionService {
    
    List<SportsAuctionConfig> getConfigsBySportAndCommunity(Long sportId, Long communityId);
    List<SportsAuctionConfig> getAllConfigsByCommunity(Long communityId);
    SportsAuctionConfigResponse getConfigResponse(Long id);
    List<SportsAuctionConfigResponse> getConfigResponsesBySportAndCommunity(Long sportId, Long communityId);
    List<SportsAuctionConfigResponse> getConfigResponsesByCommunity(Long communityId);
    SportsAuctionConfig createConfig(SportsAuctionConfigRequest req, Long adminUserId);
    SportsAuctionConfig updateConfig(Long configId, SportsAuctionConfigRequest req);
    SportsAuctionConfig updateStatus(Long configId, String status);
    
    // Live Auction actions
    SportsAuctionBid placeBid(BidRequest req, Long biddingUserId);
    SportsAuctionPlayer soldPlayer(SoldPlayerRequest req, Long adminUserId);
    SportsAuctionPlayer passPlayer(Long playerId, Long adminUserId);
    // History and Lists
    SportsAuctionStatsResponse getAuctionStats(Long configId);
    long getConfirmedRegistrationCount(Long configId);
    List<SportsAuctionBid> getBidHistory(Long playerId);
    List<SportsAuctionPlayer> getPlayers(Long configId, String category, String status);

    PlayerWithBidResponse getCurrentPlayer(Long configId);
    PlayerWithBidResponse pickRandomPlayer(Long configId);
    
    SportsAuctionPlayer createPlayer(Long configId, SportsAuctionPlayerRequest req);
}
