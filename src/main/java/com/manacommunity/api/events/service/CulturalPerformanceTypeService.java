package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.CulturalPerformanceType;
import java.util.List;

public interface CulturalPerformanceTypeService {

    List<CulturalPerformanceType> getAllPerformanceTypes(Long communityId);

    CulturalPerformanceType createPerformanceType(Long communityId, String name, String description);
}
