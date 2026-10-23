package com.manacommunity.api.helpdesk.scheduler;

import com.manacommunity.api.helpdesk.engine.HelpdeskSlaEngine;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.TicketComment;
import com.manacommunity.api.helpdesk.entity.TicketSlaRule;
import com.manacommunity.api.helpdesk.repository.TicketCommentRepository;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.helpdesk.repository.TicketSlaRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HelpdeskEscalationScheduler {

    private final TicketRepository ticketRepository;
    private final TicketSlaRuleRepository slaRuleRepository;
    private final TicketCommentRepository commentRepository;
    private final HelpdeskSlaEngine slaEngine;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void monitorAndEscalateTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> activeTickets = ticketRepository.findByStatusIn(List.of(Ticket.TicketStatus.OPEN, Ticket.TicketStatus.IN_PROGRESS));
        if (activeTickets.isEmpty()) {
            return;
        }

        log.debug("Evaluating SLA and escalations for {} active helpdesk tickets", activeTickets.size());

        for (Ticket ticket : activeTickets) {
            try {
                processTicketSla(ticket, now);
            } catch (Exception e) {
                log.error("Failed to process SLA escalation for ticket {}: {}", ticket.getTicketNumber(), e.getMessage(), e);
            }
        }
    }

    public void processTicketSla(Ticket ticket, LocalDateTime now) {
        Ticket.SlaStatus currentSlaStatus = slaEngine.evaluateSlaStatus(ticket, now);
        boolean changed = false;

        if (ticket.getSlaStatus() != currentSlaStatus) {
            ticket.setSlaStatus(currentSlaStatus);
            changed = true;
        }

        TicketSlaRule rule = slaRuleRepository.findByCategoryAndPriorityAndCommunityIdAndActiveTrue(
                ticket.getCategory(), ticket.getPriority(), ticket.getCommunity() != null ? ticket.getCommunity().getId() : null)
                .or(() -> slaRuleRepository.findByCategoryAndPriorityAndCommunityIsNullAndActiveTrue(
                        ticket.getCategory(), ticket.getPriority()))
                .orElse(null);

        int targetLevel = slaEngine.evaluateEscalationLevel(ticket, now, rule);
        if (targetLevel > ticket.getEscalationLevel()) {
            ticket.setEscalated(true);
            ticket.setEscalationLevel(targetLevel);
            ticket.setEscalatedAt(now);
            ticket.setLastEscalatedAt(now);
            changed = true;

            TicketComment escalationComment = TicketComment.builder()
                    .ticket(ticket)
                    .author(null)
                    .message("Auto-Escalation: Ticket exceeded SLA thresholds and has been escalated to Level " + targetLevel + ".")
                    .createdAt(now)
                    .build();
            commentRepository.save(escalationComment);
            log.warn("Ticket {} auto-escalated to Level {}", ticket.getTicketNumber(), targetLevel);
        }

        if (changed) {
            ticketRepository.save(ticket);
        }
    }
}
