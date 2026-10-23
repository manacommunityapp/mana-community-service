package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventCulturalCategory;
import java.util.List;

public interface CulturalCategoryService {

    List<EventCulturalCategory> getAllCulturalCategories(Long communityId);

    EventCulturalCategory createCulturalCategory(Long communityId, String name, String description);

    EventCulturalCategory updateCulturalCategory(Long id, Long communityId, String name, String description);

    void deleteCulturalCategory(Long id, Long communityId);
}
