package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventCompetitionCategory;
import java.util.List;

public interface CompetitionCategoryService {

    List<EventCompetitionCategory> getAllCompetitionCategories(Long communityId);

    EventCompetitionCategory createCompetitionCategory(Long communityId, String name, String description);
}
