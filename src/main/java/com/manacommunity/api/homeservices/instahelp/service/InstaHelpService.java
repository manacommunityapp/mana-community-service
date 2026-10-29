package com.manacommunity.api.homeservices.instahelp.service;

import com.manacommunity.api.homeservices.instahelp.dto.InstaHelpRequest;
import com.manacommunity.api.homeservices.instahelp.dto.InstaHelpResponse;
import com.manacommunity.api.homeservices.instahelp.dto.RateRequest;

import java.util.List;

public interface InstaHelpService {

    InstaHelpResponse create(Long communityId, Long residentId, InstaHelpRequest request);

    List<InstaHelpResponse> getMyBookings(Long residentId);

    InstaHelpResponse startSession(String bookingRef, Long residentId);

    InstaHelpResponse completeSession(String bookingRef, Long residentId);

    InstaHelpResponse cancel(String bookingRef, Long residentId);

    InstaHelpResponse rate(String bookingRef, Long residentId, RateRequest req);
}
