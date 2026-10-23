package com.manacommunity.api.intelligence.repository;

import com.manacommunity.api.intelligence.model.GraphProfile;
import com.manacommunity.api.intelligence.model.ProfileVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraphProfileRepository extends JpaRepository<GraphProfile, Long> {

    Optional<GraphProfile> findByUserId(Long userId);

    List<GraphProfile> findByCommunityIdAndIsActiveTrue(Long communityId);

    @Query("SELECT p FROM GraphProfile p WHERE p.community.id = :communityId AND p.isActive = true AND p.visibility != 'PRIVATE'")
    List<GraphProfile> findDiscoverableProfiles(@Param("communityId") Long communityId);

    @Query("SELECT p FROM GraphProfile p WHERE p.community.id = :communityId AND p.isActive = true AND p.visibility = :visibility")
    List<GraphProfile> findByCommunityIdAndVisibility(@Param("communityId") Long communityId, @Param("visibility") ProfileVisibility visibility);
}