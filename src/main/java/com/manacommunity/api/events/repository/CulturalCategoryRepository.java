package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCulturalCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CulturalCategoryRepository extends JpaRepository<EventCulturalCategory, Long> {

    List<EventCulturalCategory> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<EventCulturalCategory> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
