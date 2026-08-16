package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.CulturalCategory;
import java.util.List;

public interface CulturalCategoryService {

    List<CulturalCategory> getAllCulturalCategories(Long communityId);

    CulturalCategory createCulturalCategory(Long communityId, String name, String description);
}
