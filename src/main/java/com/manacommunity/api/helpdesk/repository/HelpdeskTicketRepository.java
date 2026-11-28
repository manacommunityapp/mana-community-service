package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.HelpdeskTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, Long> {

    List<HelpdeskTicket> findByCommunityId(Long communityId);

    List<HelpdeskTicket> findByCommunityIdAndStatus(Long communityId, String status);

    List<HelpdeskTicket> findByCommunityIdAndStatusIn(Long communityId, List<String> statuses);

    List<HelpdeskTicket> findByStatusIn(List<String> statuses);

    List<HelpdeskTicket> findByParentIncidentId(Long parentIncidentId);

    @Query("SELECT t FROM HelpdeskTicket t WHERE t.status IN ('OPEN','IN_PROGRESS') AND t.slaPaused = false AND t.slaDeadline < :now")
    List<HelpdeskTicket> findBreachedTickets(@Param("now") Instant now);

    @Query("SELECT t FROM HelpdeskTicket t WHERE t.communityId = :communityId AND t.category = :category AND t.tower = :tower AND t.status IN ('OPEN','IN_PROGRESS') AND t.createdAt > :since")
    List<HelpdeskTicket> findRecentSimilarTickets(@Param("communityId") Long communityId,
                                                    @Param("category") String category,
                                                    @Param("tower") String tower,
                                                    @Param("since") Instant since);

    List<HelpdeskTicket> findByAssignedTechnicianIdAndStatusIn(Long technicianId, List<String> statuses);

    long countByCommunityIdAndStatus(Long communityId, String status);
}
