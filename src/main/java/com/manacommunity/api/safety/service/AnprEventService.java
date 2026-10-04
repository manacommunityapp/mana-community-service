package com.manacommunity.api.safety.service;

import com.manacommunity.api.safety.dto.AnprEventResponse;
import com.manacommunity.api.safety.dto.AnprSummaryResponse;
import com.manacommunity.api.safety.model.AnprEvent;

import java.util.List;

public interface AnprEventService {

    List<AnprEventResponse> getEvents(Long communityId, String gate, AnprEvent.Direction direction);

    AnprEventResponse getEventById(Long id);

    AnprSummaryResponse getSummary(Long communityId);
}
