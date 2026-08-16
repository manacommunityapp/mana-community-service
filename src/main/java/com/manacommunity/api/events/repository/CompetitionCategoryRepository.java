package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.CompetitionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionCategoryRepository extends JpaRepository<CompetitionCategory, Long> {

    List<CompetitionCategory> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<CompetitionCategory> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
