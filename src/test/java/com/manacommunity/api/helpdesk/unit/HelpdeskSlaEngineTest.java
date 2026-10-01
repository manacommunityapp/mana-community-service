package com.manacommunity.api.helpdesk.unit;

import com.manacommunity.api.helpdesk.dto.HelpdeskAnalyticsResponse;
import com.manacommunity.api.helpdesk.engine.HelpdeskSlaEngine;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.entity.TicketSlaRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Helpdesk SLA Engine Unit Tests")
class HelpdeskSlaEngineTest {

    private HelpdeskSlaEngine engine;

    @BeforeEach
    void setUp() {
        engine = new HelpdeskSlaEngine();
    }

    @Test
    @DisplayName("Should calculate SLA due date based on priority")
    void testCalculateSlaDueDateDefault() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);

        LocalDateTime dueCritical = engine.calculateSlaDueDate(now, Ticket.TicketPriority.CRITICAL, null);
        assertEquals(now.plusHours(2), dueCritical);

        LocalDateTime dueHigh = engine.calculateSlaDueDate(now, Ticket.TicketPriority.HIGH, null);
        assertEquals(now.plusHours(6), dueHigh);

        LocalDateTime dueMedium = engine.calculateSlaDueDate(now, Ticket.TicketPriority.MEDIUM, null);
        assertEquals(now.plusHours(24), dueMedium);
    }

    @Test
    @DisplayName("Should apply custom SLA rule when present")
    void testCalculateSlaDueDateCustomRule() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 10, 0);
        TicketSlaRule customRule = TicketSlaRule.builder()
                .resolutionTimeHours(4)
                .build();

        LocalDateTime due = engine.calculateSlaDueDate(now, Ticket.TicketPriority.HIGH, customRule);
        assertEquals(now.plusHours(4), due);
    }

    @Test
    @DisplayName("Should correctly detect SLA breach")
    void testIsSlaBreached() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0);
        Ticket ticket = Ticket.builder()
                .slaDueAt(now.minusHours(1))
                .status(Ticket.TicketStatus.OPEN)
                .build();

        assertTrue(engine.isSlaBreached(ticket, now));

        ticket.setSlaDueAt(now.plusHours(1));
        assertFalse(engine.isSlaBreached(ticket, now));
    }

    @Test
    @DisplayName("Should evaluate escalation level accurately")
    void testEvaluateEscalationLevel() {
        LocalDateTime created = LocalDateTime.of(2026, 10, 1, 8, 0);
        Ticket ticket = Ticket.builder()
                .priority(Ticket.TicketPriority.CRITICAL)
                .status(Ticket.TicketStatus.OPEN)
                .build();
        ticket.setCreatedAt(created);

        // 3 hours later -> level 2
        int level = engine.evaluateEscalationLevel(ticket, created.plusHours(3), null);
        assertEquals(2, level);
    }

    @Test
    @DisplayName("Should calculate helpdesk analytics metrics")
    void testCalculateAnalytics() {
        LocalDateTime now = LocalDateTime.now();
        Ticket t1 = Ticket.builder()
                .category(Ticket.TicketCategory.PLUMBING)
                .priority(Ticket.TicketPriority.CRITICAL)
                .status(Ticket.TicketStatus.RESOLVED)
                .resolvedAt(now)
                .slaDueAt(now.plusHours(1))
                .satisfactionRating(5)
                .build();
        t1.setCreatedAt(now.minusHours(1));

        Ticket t2 = Ticket.builder()
                .category(Ticket.TicketCategory.ELECTRICAL)
                .priority(Ticket.TicketPriority.HIGH)
                .status(Ticket.TicketStatus.OPEN)
                .slaDueAt(now.plusHours(3))
                .escalated(true)
                .build();
        t2.setCreatedAt(now.minusHours(3));

        List<Ticket> tickets = List.of(t1, t2);

        HelpdeskAnalyticsResponse res = engine.calculateAnalytics(tickets);
        assertNotNull(res);
        assertEquals(2, res.getTotalTickets());
        assertEquals(1, res.getOpenTickets());
        assertEquals(1, res.getResolvedTickets());
        assertEquals(1, res.getEscalatedTickets());
        assertEquals(100.0, res.getSlaCompliancePercentage());
        assertEquals(5.0, res.getAverageCsatRating());
    }
}
