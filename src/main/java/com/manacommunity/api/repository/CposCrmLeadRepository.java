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

    @Query("SELECT l FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false AND l.id = :id")
    Optional<CposCrmLead> findByCommunityIdAndId(@Param("communityId") Long communityId, @Param("id") Long id);

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

    @Query("SELECT l FROM CposCrmLead l WHERE l.deleted = false " +
           "AND l.followUpAt IS NOT NULL " +
           "AND l.followUpAt BETWEEN :from AND :to")
    List<CposCrmLead> findDueForNotification(
            @Param("from") java.time.LocalDateTime from,
            @Param("to")   java.time.LocalDateTime to);

    boolean existsByCommunityIdAndPhoneAndDeletedFalse(Long communityId, String phone);
    boolean existsByCommunityIdAndEmailAndDeletedFalse(Long communityId, String email);

    @Query("SELECT l FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false AND l.updatedAt < :cutoff " +
           "AND l.stage NOT IN ('CLOSED') " +
           "AND l.assignedTo IS NOT NULL")
    List<CposCrmLead> findStaleLeads(
            @Param("communityId") Long communityId,
            @Param("cutoff") java.time.LocalDateTime cutoff);

    @Query("SELECT l FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false " +
           "AND (:stage IS NULL OR l.stage = :stage) " +
           "AND (:source IS NULL OR l.source = :source) " +
           "AND l.email IS NOT NULL AND l.email != '' " +
           "ORDER BY l.createdAt DESC")
    List<CposCrmLead> findForEmailCampaign(
            @Param("communityId") Long communityId,
            @Param("stage") String stage,
            @Param("source") String source);

    @Query("SELECT COUNT(l) FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false AND l.stage = 'CLOSED' " +
           "AND l.assignedTo = :agentName " +
           "AND MONTH(l.updatedAt) = :month AND YEAR(l.updatedAt) = :year")
    long countClosedByAgentInMonth(@Param("communityId") Long communityId,
                                    @Param("agentName") String agentName,
                                    @Param("month") int month,
                                    @Param("year") int year);

    @Query("SELECT SUM(l.budgetLakhs) FROM CposCrmLead l WHERE l.community.id = :communityId " +
           "AND l.deleted = false AND l.stage = 'CLOSED' " +
           "AND l.assignedTo = :agentName " +
           "AND MONTH(l.updatedAt) = :month AND YEAR(l.updatedAt) = :year")
    Long sumRevenueByAgentInMonth(@Param("communityId") Long communityId,
                                   @Param("agentName") String agentName,
                                   @Param("month") int month,
                                   @Param("year") int year);

    @Query("SELECT l.assignedTo, COUNT(l), SUM(l.budgetLakhs) FROM CposCrmLead l " +
           "WHERE l.community.id = :communityId AND l.deleted = false AND l.stage = 'CLOSED' " +
           "AND MONTH(l.updatedAt) = :month AND YEAR(l.updatedAt) = :year " +
           "GROUP BY l.assignedTo ORDER BY COUNT(l) DESC")
    List<Object[]> leaderboardForMonth(@Param("communityId") Long communityId,
                                        @Param("month") int month,
                                        @Param("year") int year);
}
