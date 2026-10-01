package com.manacommunity.api.sports.service;

import com.manacommunity.api.sports.dto.VenueRequest;
import com.manacommunity.api.sports.dto.VenueResponse;
import com.manacommunity.api.sports.model.Venue;

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
