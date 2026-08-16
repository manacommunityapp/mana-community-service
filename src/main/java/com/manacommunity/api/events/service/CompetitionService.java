package com.manacommunity.api.events.service;

import com.manacommunity.api.events.entity.Competition;
import java.util.List;

public interface CompetitionService {

    List<Competition> getAllCompetitions(Long communityId, Long mainEventId);

    Competition getCompetitionById(Long id, Long communityId);

    Competition createCompetition(Long communityId, Competition competition);

    Competition updateCompetition(Long id, Long communityId, Competition competition);

    void deleteCompetition(Long id, Long communityId);
}
