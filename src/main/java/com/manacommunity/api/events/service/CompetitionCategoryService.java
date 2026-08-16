package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.CompetitionCategory;
import java.util.List;

public interface CompetitionCategoryService {

    List<CompetitionCategory> getAllCompetitionCategories(Long communityId);

    CompetitionCategory createCompetitionCategory(Long communityId, String name, String description);
}
