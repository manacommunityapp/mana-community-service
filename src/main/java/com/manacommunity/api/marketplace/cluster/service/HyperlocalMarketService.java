package com.manacommunity.api.marketplace.cluster.service;

import com.manacommunity.api.marketplace.cluster.dto.MarketClusterDtos.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface HyperlocalMarketService {

    RankedListingResponse createListing(CreateClusterListingRequest req, AppUser seller, Community community);

    List<RankedListingResponse> getRankedHyperlocalFeed(HyperlocalFeedQuery query);

    String computeS2CellIdL13(double latitude, double longitude);

    List<String> getS2ClusterNeighborTokens(double latitude, double longitude);
}
