package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.EventCompetition;
import java.util.List;

public interface CompetitionService {

    List<EventCompetition> getAllCompetitions(Long communityId, Long mainEventId);

    EventCompetition getCompetitionById(Long id, Long communityId);

    EventCompetition createCompetition(Long communityId, EventCompetition competition);

    EventCompetition updateCompetition(Long id, Long communityId, EventCompetition competition);

    void deleteCompetition(Long id, Long communityId);
}
