package com.manacommunity.api.helpdesk.repository;

import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.TicketSlaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketSlaRuleRepository extends JpaRepository<TicketSlaRule, Long> {
    Optional<TicketSlaRule> findByCategoryAndPriorityAndCommunityIdAndActiveTrue(
            Ticket.TicketCategory category, Ticket.TicketPriority priority, Long communityId);

    Optional<TicketSlaRule> findByCategoryAndPriorityAndCommunityIsNullAndActiveTrue(
            Ticket.TicketCategory category, Ticket.TicketPriority priority);
}
