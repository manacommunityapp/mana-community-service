package com.manacommunity.api.repository;

import com.manacommunity.api.model.CommunityLeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityLeaderRepository extends JpaRepository<CommunityLeader, Long> {

    List<CommunityLeader> findByCommunityIdAndIsActiveTrueOrderByDisplayOrderAsc(Long communityId);

    List<CommunityLeader> findByCommunityIdOrderByDisplayOrderAsc(Long communityId);

    boolean existsByCommunityIdAndUserIdAndDesignation(Long communityId, Long userId, String designation);

    @Query("""
            SELECT l FROM CommunityLeader l JOIN FETCH l.user u
            WHERE l.community.id = :communityId
              AND l.isActive = true
              AND l.designation = :designation
            ORDER BY l.displayOrder ASC
            """)
    List<CommunityLeader> findByDesignation(
            @Param("communityId") Long communityId,
            @Param("designation") String designation);

    @Query("""
            SELECT l FROM CommunityLeader l JOIN FETCH l.user u
            WHERE l.community.id = :communityId
              AND l.isActive = true
              AND l.committee = :committee
            ORDER BY l.displayOrder ASC
            """)
    List<CommunityLeader> findByCommittee(
            @Param("communityId") Long communityId,
            @Param("committee") String committee);

    @Query("""
            SELECT DISTINCT l.committee FROM CommunityLeader l
            WHERE l.community.id = :communityId
              AND l.isActive = true
              AND l.committee IS NOT NULL
            ORDER BY l.committee ASC
            """)
    List<String> findDistinctCommittees(@Param("communityId") Long communityId);

    long countByCommunityIdAndIsActiveTrue(Long communityId);

    List<CommunityLeader> findByUserIdAndIsActiveTrue(Long userId);
}
