package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByRaisedByIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    @Query("SELECT t FROM Ticket t WHERE t.community.id = :communityId " +
           "AND t.status IN :statuses ORDER BY t.priority DESC, t.createdAt DESC")
    List<Ticket> findByCommunityAndStatuses(
            @Param("communityId") Long communityId,
            @Param("statuses") List<Ticket.TicketStatus> statuses);

    @Query("SELECT t FROM Ticket t WHERE t.community.id = :communityId " +
           "AND t.category = :category ORDER BY t.createdAt DESC")
    List<Ticket> findByCommunityAndCategory(
            @Param("communityId") Long communityId,
            @Param("category") Ticket.TicketCategory category);

    long countByCommunityIdAndStatus(Long communityId, Ticket.TicketStatus status);
}
