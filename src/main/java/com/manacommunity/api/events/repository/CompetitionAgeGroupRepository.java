package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.CompetitionAgeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionAgeGroupRepository extends JpaRepository<CompetitionAgeGroup, Long> {

    List<CompetitionAgeGroup> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<CompetitionAgeGroup> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
