package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.CulturalEvent;
import java.util.List;

public interface CulturalEventService {

    List<CulturalEvent> getAllCulturalEvents(Long communityId, Long mainEventId);

    CulturalEvent getCulturalEventById(Long id, Long communityId);

    CulturalEvent createCulturalEvent(Long communityId, CulturalEvent culturalEvent);

    CulturalEvent updateCulturalEvent(Long id, Long communityId, CulturalEvent culturalEvent);

    void deleteCulturalEvent(Long id, Long communityId);
}
