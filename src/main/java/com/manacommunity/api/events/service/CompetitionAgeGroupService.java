package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.CompetitionAgeGroup;
import java.util.List;

public interface CompetitionAgeGroupService {

    List<CompetitionAgeGroup> getAllCompetitionAgeGroups(Long communityId);

    CompetitionAgeGroup createCompetitionAgeGroup(Long communityId, String name, String description);
}
