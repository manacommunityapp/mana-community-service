package com.manacommunity.api.helpdesk.unit;

import com.manacommunity.api.helpdesk.dto.HelpdeskAiDtos.AiClassificationResult;
import com.manacommunity.api.helpdesk.engine.HelpdeskAiClassificationEngine;
import com.manacommunity.api.helpdesk.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Helpdesk AI Classification Engine Unit Tests")
class HelpdeskAiClassificationEngineTest {

    private HelpdeskAiClassificationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new HelpdeskAiClassificationEngine();
    }

    @Test
    @DisplayName("Should detect safety hazard (gas leak) and override priority to CRITICAL")
    void testDetectGasLeakHazard() {
        AiClassificationResult result = engine.classifyTicket("Gas smell in basement", "Strong LPG cylinder smell near kitchen pipe line");

        assertNotNull(result);
        assertTrue(result.isSafetyHazard(), "Must flag as safety hazard");
        assertEquals(Ticket.TicketPriority.CRITICAL, result.suggestedPriority());
        assertTrue(result.urgencyScore() >= 90);
    }

    @Test
    @DisplayName("Should detect electrical sparking hazard")
    void testDetectElectricalHazard() {
        AiClassificationResult result = engine.classifyTicket("Sparks in meter box", "Sparks and burning smell from main electrical panel");

        assertNotNull(result);
        assertTrue(result.isSafetyHazard());
        assertEquals(Ticket.TicketPriority.CRITICAL, result.suggestedPriority());
        assertEquals(Ticket.TicketCategory.ELECTRICAL, result.category());
        assertTrue(result.requiredSkills().contains("ELECTRICIAN"));
    }

    @Test
    @DisplayName("Should detect elevator entrapment hazard")
    void testDetectElevatorEntrapment() {
        AiClassificationResult result = engine.classifyTicket("Lift stuck with people inside", "Passengers stuck between 3rd and 4th floor, lift stuck");

        assertNotNull(result);
        assertTrue(result.isSafetyHazard());
        assertEquals(Ticket.TicketPriority.CRITICAL, result.suggestedPriority());
        assertEquals(Ticket.TicketCategory.ELEVATOR, result.category());
        assertTrue(result.requiredSkills().contains("ELEVATOR_MAINTENANCE"));
    }

    @Test
    @DisplayName("Should classify standard plumbing issue without hazard flag")
    void testClassifyStandardPlumbing() {
        AiClassificationResult result = engine.classifyTicket("Tap dripping", "Bathroom tap is dripping slowly since yesterday");

        assertNotNull(result);
        assertFalse(result.isSafetyHazard());
        assertEquals(Ticket.TicketCategory.PLUMBING, result.category());
        assertEquals(Ticket.TicketPriority.LOW, result.suggestedPriority());
        assertTrue(result.requiredSkills().contains("PLUMBER"));
    }
}
