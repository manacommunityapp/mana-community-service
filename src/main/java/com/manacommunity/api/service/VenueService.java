package com.manacommunity.api.service;

import com.manacommunity.api.model.Venue;
import java.util.List;

public interface VenueService {
    List<Venue> getVenuesByCommunityId(Long communityId);
    List<Venue> getAllVenues();
    Venue getVenueById(Long id);
    Venue createVenue(Long communityId, Venue venue);
    Venue updateVenue(Long id, Venue venue);
    void deleteVenue(Long id);
}
