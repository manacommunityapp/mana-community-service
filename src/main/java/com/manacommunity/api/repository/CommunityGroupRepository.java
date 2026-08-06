package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommunityGroupRepository extends JpaRepository<CommunityGroup, Long> {
    Page<CommunityGroup> findByCommunityIdAndActiveTrue(Long communityId, Pageable pageable);

    Page<CommunityGroup> findByCommunityIdAndActiveTrueAndGroupTypeIn(Long communityId, List<String> groupTypes, Pageable pageable);

    Optional<CommunityGroup> findBySlugAndCommunityId(String slug, Long communityId);

    @Query("SELECT g FROM CommunityGroup g WHERE g.community.id = :communityId AND g.active = true AND LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<CommunityGroup> searchGroups(Long communityId, String query, Pageable pageable);

    @Query("SELECT g FROM CommunityGroup g WHERE g.community.id = :communityId AND g.active = true AND g.category = :category")
    Page<CommunityGroup> findByCategory(Long communityId, String category, Pageable pageable);

    long countByCommunityIdAndActiveTrue(Long communityId);
}
