package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventVenue;

import java.util.List;

public interface EventVenueService {
    List<EventVenue> getVenues(Long communityId, String status);
    EventVenue getVenueById(Long id);
    EventVenue createVenue(EventVenue venue, Long communityId);
    EventVenue updateVenue(Long id, EventVenue venue);
    void deleteVenue(Long id);
}