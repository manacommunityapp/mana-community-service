package com.manacommunity.api.repository;

import com.manacommunity.api.model.CposCrmLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CposCrmLeadRepository extends JpaRepository<CposCrmLead, Long> {

    List<CposCrmLead> findByCommunityIdAndDeletedFalseOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT l FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false " +
           "AND (:stage IS NULL OR l.stage = :stage) " +
           "ORDER BY l.createdAt DESC")
    Page<CposCrmLead> findByCommunityAndStage(
            @Param("communityId") Long communityId,
            @Param("stage") String stage,
            Pageable pageable);

    @Query("SELECT l FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false AND l.id = :id")
    Optional<CposCrmLead> findByIdAndCommunity(@Param("id") Long id, @Param("communityId") Long communityId);

    @Query("SELECT l.stage, COUNT(l) FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false GROUP BY l.stage")
    List<Object[]> countByStage(@Param("communityId") Long communityId);

    @Query("SELECT l.assignedTo, COUNT(l), SUM(l.budgetLakhs) FROM CposCrmLead l " +
           "WHERE l.community.id = :communityId AND l.deleted = false " +
           "GROUP BY l.assignedTo")
    List<Object[]> agentPerformance(@Param("communityId") Long communityId);

    @Query("SELECT l.source, COUNT(l) FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false GROUP BY l.source")
    List<Object[]> countBySource(@Param("communityId") Long communityId);

    @Query("SELECT l.stage, AVG(l.daysInStage) FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false GROUP BY l.stage")
    List<Object[]> avgDaysInStage(@Param("communityId") Long communityId);

    long countByCommunityIdAndDeletedFalse(Long communityId);

    List<CposCrmLead> findByCommunityIdAndAssignedToAndDeletedFalseOrderByCreatedAtDesc(
            Long communityId, String assignedTo);

    @Query("SELECT l FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false AND l.assignedTo = :assignedTo " +
           "AND (:stage IS NULL OR l.stage = :stage) " +
           "ORDER BY l.createdAt DESC")
    Page<CposCrmLead> findByCommunityAndAssignedToAndStage(
            @Param("communityId") Long communityId,
            @Param("assignedTo") String assignedTo,
            @Param("stage") String stage,
            Pageable pageable);

    @Query("SELECT DISTINCT l.assignedTo FROM CposCrmLead l " +
           "WHERE l.community.id = :communityId AND l.deleted = false " +
           "AND l.assignedTo IS NOT NULL ORDER BY l.assignedTo")
    List<String> findDistinctAgents(@Param("communityId") Long communityId);
}
