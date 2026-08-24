package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCompetitionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionCategoryRepository extends JpaRepository<EventCompetitionCategory, Long> {

    List<EventCompetitionCategory> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<EventCompetitionCategory> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
