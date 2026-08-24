package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCompetition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionRepository extends JpaRepository<EventCompetition, Long> {

    List<EventCompetition> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<EventCompetition> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);
}
