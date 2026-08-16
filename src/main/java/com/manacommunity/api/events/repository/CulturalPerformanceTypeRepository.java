package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.CulturalPerformanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CulturalPerformanceTypeRepository extends JpaRepository<CulturalPerformanceType, Long> {

    List<CulturalPerformanceType> findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(Long communityId);

    Optional<CulturalPerformanceType> findByNameIgnoreCaseAndCommunityId(String name, Long communityId);

    boolean existsByNameIgnoreCase(String name);
}
