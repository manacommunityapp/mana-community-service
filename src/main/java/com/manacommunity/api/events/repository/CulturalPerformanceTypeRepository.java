package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.EventCulturalPerformanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CulturalPerformanceTypeRepository extends JpaRepository<EventCulturalPerformanceType, Long> {

    List<EventCulturalPerformanceType> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<EventCulturalPerformanceType> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
