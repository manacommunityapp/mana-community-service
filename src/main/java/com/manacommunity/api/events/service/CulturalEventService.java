package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventCulturalEvent;
import java.util.List;

public interface CulturalEventService {

    List<EventCulturalEvent> getAllCulturalEvents(Long communityId, Long mainEventId);

    EventCulturalEvent getCulturalEventById(Long id, Long communityId);

    EventCulturalEvent createCulturalEvent(Long communityId, EventCulturalEvent culturalEvent);

    EventCulturalEvent updateCulturalEvent(Long id, Long communityId, EventCulturalEvent culturalEvent);

    void deleteCulturalEvent(Long id, Long communityId);
}
