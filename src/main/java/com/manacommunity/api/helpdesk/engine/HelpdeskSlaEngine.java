package com.manacommunity.api.helpdesk.engine;

import com.manacommunity.api.helpdesk.dto.HelpdeskAnalyticsResponse;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.TicketSlaRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HelpdeskSlaEngine {

    public LocalDateTime calculateSlaDueDate(LocalDateTime createdAt, Ticket.TicketPriority priority, TicketSlaRule rule) {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (rule != null && rule.getResolutionTimeHours() > 0) {
            return createdAt.plusHours(rule.getResolutionTimeHours());
        }

        int defaultHours = switch (priority != null ? priority : Ticket.TicketPriority.MEDIUM) {
            case CRITICAL -> 2;
            case HIGH -> 6;
            case MEDIUM -> 24;
            case LOW -> 72;
        };

        return createdAt.plusHours(defaultHours);
    }

    public boolean isSlaBreached(Ticket ticket, LocalDateTime currentTime) {
        if (ticket.getSlaDueAt() == null) {
            return false;
        }
        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED) {
            return ticket.getResolvedAt() != null && ticket.getResolvedAt().isAfter(ticket.getSlaDueAt());
        }
        return currentTime.isAfter(ticket.getSlaDueAt());
    }

    public Ticket.SlaStatus evaluateSlaStatus(Ticket ticket, LocalDateTime currentTime) {
        if (ticket.getSlaDueAt() == null || ticket.getCreatedAt() == null) {
            return Ticket.SlaStatus.ON_TRACK;
        }
        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED) {
            if (ticket.getResolvedAt() != null && ticket.getResolvedAt().isAfter(ticket.getSlaDueAt())) {
                return Ticket.SlaStatus.BREACHED;
            }
            return Ticket.SlaStatus.ON_TRACK;
        }

        if (currentTime.isAfter(ticket.getSlaDueAt())) {
            return Ticket.SlaStatus.BREACHED;
        }

        long totalDurationSeconds = Duration.between(ticket.getCreatedAt(), ticket.getSlaDueAt()).getSeconds();
        if (totalDurationSeconds <= 0) {
            return Ticket.SlaStatus.BREACHED;
        }

        long elapsedSeconds = Duration.between(ticket.getCreatedAt(), currentTime).getSeconds();
        double ratio = (double) elapsedSeconds / (double) totalDurationSeconds;

        if (ratio >= 1.0) {
            return Ticket.SlaStatus.BREACHED;
        } else if (ratio >= 0.75) {
            return Ticket.SlaStatus.AT_RISK;
        } else {
            return Ticket.SlaStatus.ON_TRACK;
        }
    }

    public int evaluateEscalationLevel(Ticket ticket, LocalDateTime currentTime, TicketSlaRule rule) {
        if (ticket.getStatus() == Ticket.TicketStatus.RESOLVED || ticket.getStatus() == Ticket.TicketStatus.CLOSED || ticket.getStatus() == Ticket.TicketStatus.REJECTED) {
            return 0;
        }
        LocalDateTime created = ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now();
        long hoursElapsed = Duration.between(created, currentTime).toHours();

        int l1 = rule != null ? rule.getEscalationLevel1Hours() : (ticket.getPriority() == Ticket.TicketPriority.CRITICAL ? 1 : 12);
        int l2 = rule != null ? rule.getEscalationLevel2Hours() : (ticket.getPriority() == Ticket.TicketPriority.CRITICAL ? 2 : 24);

        if (hoursElapsed >= l2) {
            return 2;
        } else if (hoursElapsed >= l1) {
            return 1;
        }
        return 0;
    }

    public HelpdeskAnalyticsResponse calculateAnalytics(List<Ticket> tickets) {
        long total = tickets.size();
        long open = 0, inProgress = 0, resolved = 0, closed = 0, escalated = 0;
        long compliantCount = 0;
        double totalResolutionHours = 0;
        int resolvedCount = 0;
        double totalCsat = 0;
        int csatCount = 0;

        Map<String, Long> byCategory = new HashMap<>();
        Map<String, Long> byPriority = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();

        for (Ticket t : tickets) {
            String cat = t.getCategory().name();
            byCategory.put(cat, byCategory.getOrDefault(cat, 0L) + 1);

            String pri = t.getPriority().name();
            byPriority.put(pri, byPriority.getOrDefault(pri, 0L) + 1);

            if (t.isEscalated()) escalated++;

            switch (t.getStatus()) {
                case OPEN -> open++;
                case IN_PROGRESS -> inProgress++;
                case RESOLVED -> resolved++;
                case CLOSED -> closed++;
                default -> {}
            }

            if (t.getSatisfactionRating() != null && t.getSatisfactionRating() > 0) {
                totalCsat += t.getSatisfactionRating();
                csatCount++;
            }

            if (t.getResolvedAt() != null && t.getCreatedAt() != null) {
                resolvedCount++;
                Duration d = Duration.between(t.getCreatedAt(), t.getResolvedAt());
                totalResolutionHours += d.toMinutes() / 60.0;
                if (t.getSlaDueAt() != null && !t.getResolvedAt().isAfter(t.getSlaDueAt())) {
                    compliantCount++;
                }
            } else if (t.getSlaDueAt() != null && !now.isAfter(t.getSlaDueAt())) {
                compliantCount++;
            }
        }

        double compliance = total > 0 ? (compliantCount * 100.0 / total) : 100.0;
        double avgResolution = resolvedCount > 0 ? (totalResolutionHours / resolvedCount) : 0.0;
        double avgCsat = csatCount > 0 ? (totalCsat / csatCount) : 5.0;

        return HelpdeskAnalyticsResponse.builder()
                .totalTickets(total)
                .openTickets(open)
                .inProgressTickets(inProgress)
                .resolvedTickets(resolved)
                .closedTickets(closed)
                .escalatedTickets(escalated)
                .slaCompliancePercentage(BigDecimal.valueOf(compliance).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .averageResolutionTimeHours(BigDecimal.valueOf(avgResolution).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .averageCsatRating(BigDecimal.valueOf(avgCsat).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .ticketsByCategory(byCategory)
                .ticketsByPriority(byPriority)
                .build();
    }
}
