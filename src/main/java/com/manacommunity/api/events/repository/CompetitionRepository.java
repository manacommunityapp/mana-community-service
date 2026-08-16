package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    List<Competition> findByCommunityIdOrderByDateAscStartTimeAsc(Long communityId);

    List<Competition> findByMainEventIdOrderByDateAscStartTimeAsc(Long mainEventId);
}
