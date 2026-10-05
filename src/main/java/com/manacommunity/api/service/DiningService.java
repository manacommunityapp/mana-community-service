package com.manacommunity.api.service;

import com.manacommunity.api.dto.DiningEventRequest;
import com.manacommunity.api.dto.DiningEventResponse;
import com.manacommunity.api.dto.DiningRsvpRequest;
import com.manacommunity.api.dto.DiningRsvpResponse;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface DiningService {

    List<DiningEventResponse> getCommunityEvents(Long communityId);

    DiningEventResponse getEvent(Long id, Long communityId);

    DiningEventResponse createEvent(DiningEventRequest request, AppUser user);

    DiningRsvpResponse createRsvp(DiningRsvpRequest request, AppUser user);

    void cancelRsvp(Long rsvpId, Long userId);
}
