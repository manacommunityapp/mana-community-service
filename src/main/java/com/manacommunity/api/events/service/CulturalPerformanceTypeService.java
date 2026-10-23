package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventCulturalPerformanceType;
import java.util.List;

public interface CulturalPerformanceTypeService {

    List<EventCulturalPerformanceType> getAllPerformanceTypes(Long communityId);

    EventCulturalPerformanceType createPerformanceType(Long communityId, String name, String description);

    EventCulturalPerformanceType updatePerformanceType(Long id, Long communityId, String name, String description);

    void deletePerformanceType(Long id, Long communityId);
}
