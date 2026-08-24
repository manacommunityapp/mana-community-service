package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCompetitionAgeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionAgeGroupRepository extends JpaRepository<EventCompetitionAgeGroup, Long> {

    List<EventCompetitionAgeGroup> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<EventCompetitionAgeGroup> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
