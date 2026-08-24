package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventCompetitionAgeGroup;
import java.util.List;

public interface CompetitionAgeGroupService {

    List<EventCompetitionAgeGroup> getAllCompetitionAgeGroups(Long communityId);

    EventCompetitionAgeGroup createCompetitionAgeGroup(Long communityId, String name, String description);
}
