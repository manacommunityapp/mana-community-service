package com.manacommunity.api.service;

import com.manacommunity.api.dto.VenueRequest;
import com.manacommunity.api.dto.VenueResponse;
import com.manacommunity.api.model.Venue;

import java.util.List;

public interface VenueService {
    List<VenueResponse> getVenuesByCommunityId(Long communityId);
    List<VenueResponse> getAllVenues();
    Venue getVenueById(Long id);
    VenueResponse getVenueResponseById(Long id);
    VenueResponse createVenue(Long communityId, VenueRequest request);
    VenueResponse updateVenue(Long id, VenueRequest request);
    void deleteVenue(Long id);
}
