package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.CulturalCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CulturalCategoryRepository extends JpaRepository<CulturalCategory, Long> {

    List<CulturalCategory> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<CulturalCategory> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
