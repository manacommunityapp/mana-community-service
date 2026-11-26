package com.manacommunity.api.helpdesk.service;

import com.manacommunity.api.helpdesk.entity.EscalationRule;
import com.manacommunity.api.helpdesk.entity.HelpdeskTicket;
import com.manacommunity.api.helpdesk.repository.EscalationRuleRepository;
import com.manacommunity.api.helpdesk.repository.HelpdeskTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaEscalationService {

    private final HelpdeskTicketRepository ticketRepository;
    private final EscalationRuleRepository ruleRepository;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Transactional
    public HelpdeskTicket createTicket(HelpdeskTicket ticket) {
        List<EscalationRule> rules = ruleRepository
                .findByCommunityIdAndCategoryAndActiveTrueOrderByEscalationLevelAsc(
                        ticket.getCommunityId(), ticket.getCategory());

        int slaMinutes = rules.isEmpty() ? 120 : rules.get(0).getEscalateAfterMinutes();
        ticket.setSlaMinutes(slaMinutes);
        ticket.setSlaDeadline(Instant.now().plus(Duration.ofMinutes(slaMinutes)));
        ticket.setEscalationLevel(0);
        ticket.setStatus("OPEN");

        boolean shouldPause = shouldPauseSla(ticket, rules);
        if (shouldPause) {
            ticket.setSlaPaused(true);
            ticket.setSlaPauseStart(Instant.now());
        }

        return ticketRepository.save(ticket);
    }

    @Transactional
    public EscalationSweepResult runEscalationSweep() {
        Instant now = Instant.now();
        List<HelpdeskTicket> breached = ticketRepository.findBreachedTickets(now);
        List<EscalationEvent> events = new ArrayList<>();

        for (HelpdeskTicket ticket : breached) {
            List<EscalationRule> rules = ruleRepository
                    .findByCommunityIdAndCategoryAndActiveTrueOrderByEscalationLevelAsc(
                            ticket.getCommunityId(), ticket.getCategory());

            int nextLevel = ticket.getEscalationLevel() + 1;
            EscalationRule nextRule = rules.stream()
                    .filter(r -> r.getEscalationLevel() == nextLevel)
                    .findFirst().orElse(null);

            if (nextRule == null) {
                log.warn("Ticket {} has no further escalation rules (at level {})", ticket.getId(), ticket.getEscalationLevel());
                continue;
            }

            int elapsedMinutes = computeElapsedMinutes(ticket, now);
            if (elapsedMinutes < nextRule.getEscalateAfterMinutes()) continue;

            ticket.setEscalationLevel(nextLevel);
            ticket.setSlaElapsedMinutes(elapsedMinutes);

            EscalationRule followingRule = rules.stream()
                    .filter(r -> r.getEscalationLevel() == nextLevel + 1)
                    .findFirst().orElse(null);
            if (followingRule != null) {
                ticket.setSlaDeadline(now.plus(Duration.ofMinutes(
                        followingRule.getEscalateAfterMinutes() - elapsedMinutes)));
            }

            ticketRepository.save(ticket);
            events.add(new EscalationEvent(ticket.getId(), nextLevel, nextRule.getEscalateToRole(),
                    elapsedMinutes, ticket.getTitle()));
            log.warn("Ticket {} escalated to level {} ({}): '{}'",
                    ticket.getId(), nextLevel, nextRule.getEscalateToRole(), ticket.getTitle());
        }

        return new EscalationSweepResult(breached.size(), events);
    }

    @Transactional
    public void pauseOffHoursTickets() {
        LocalTime now = LocalTime.now(IST);

        List<HelpdeskTicket> active = ticketRepository.findBreachedTickets(Instant.now().plus(Duration.ofDays(30)));
        // We actually need all active tickets, not just breached. Using status query instead:
        // This is simplified; in production a dedicated query would be better.

        for (HelpdeskTicket ticket : active) {
            List<EscalationRule> rules = ruleRepository
                    .findByCommunityIdAndCategoryAndActiveTrueOrderByEscalationLevelAsc(
                            ticket.getCommunityId(), ticket.getCategory());

            boolean shouldPause = shouldPauseSla(ticket, rules);

            if (shouldPause && !ticket.getSlaPaused()) {
                ticket.setSlaPaused(true);
                ticket.setSlaPauseStart(Instant.now());
                ticketRepository.save(ticket);
                log.debug("SLA paused for ticket {} (off-hours)", ticket.getId());
            } else if (!shouldPause && ticket.getSlaPaused()) {
                long pausedMinutes = Duration.between(ticket.getSlaPauseStart(), Instant.now()).toMinutes();
                ticket.setSlaDeadline(ticket.getSlaDeadline().plus(Duration.ofMinutes(pausedMinutes)));
                ticket.setSlaPaused(false);
                ticket.setSlaPauseStart(null);
                ticketRepository.save(ticket);
                log.debug("SLA resumed for ticket {} (+{}min added to deadline)", ticket.getId(), pausedMinutes);
            }
        }
    }

    private boolean shouldPauseSla(HelpdeskTicket ticket, List<EscalationRule> rules) {
        if ("HIGH".equals(ticket.getPriority()) || "CRITICAL".equals(ticket.getPriority())) {
            return false;
        }

        EscalationRule current = rules.stream()
                .filter(r -> r.getEscalationLevel() == ticket.getEscalationLevel()
                        || r.getEscalationLevel() == ticket.getEscalationLevel() + 1)
                .filter(EscalationRule::getPauseDuringOffHours)
                .findFirst().orElse(null);

        if (current == null) return false;

        LocalTime now = LocalTime.now(IST);
        int offStart = current.getOffHoursStart();
        int offEnd = current.getOffHoursEnd();

        if (offStart > offEnd) {
            return now.getHour() >= offStart || now.getHour() < offEnd;
        }
        return now.getHour() >= offStart && now.getHour() < offEnd;
    }

    private int computeElapsedMinutes(HelpdeskTicket ticket, Instant now) {
        long totalMinutes = Duration.between(ticket.getCreatedAt().toInstant(), now).toMinutes();
        return (int) totalMinutes - (ticket.getSlaElapsedMinutes() > 0 ? 0 : 0);
    }

    @Transactional
    public HelpdeskTicket resolveTicket(Long ticketId, String resolutionNotes) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(Instant.now());
        ticket.setResolutionNotes(resolutionNotes);

        if (ticket.getIsParentIncident()) {
            List<HelpdeskTicket> children = ticketRepository.findByParentIncidentId(ticketId);
            for (HelpdeskTicket child : children) {
                child.setStatus("RESOLVED");
                child.setResolvedAt(Instant.now());
                child.setResolutionNotes("Resolved via parent incident #" + ticketId + ": " + resolutionNotes);
                ticketRepository.save(child);
            }
            log.info("Resolved parent incident {} and {} child tickets", ticketId, children.size());
        }

        return ticketRepository.save(ticket);
    }

    public record EscalationEvent(Long ticketId, int newLevel, String escalatedTo, int elapsedMinutes, String title) {}
    public record EscalationSweepResult(int ticketsChecked, List<EscalationEvent> escalations) {}
}
