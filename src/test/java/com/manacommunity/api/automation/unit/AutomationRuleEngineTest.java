package com.manacommunity.api.automation.unit;

import com.manacommunity.api.automation.engine.AutomationRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Mana Automation Rule Engine Unit Tests")
class AutomationRuleEngineTest {

    private AutomationRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new AutomationRuleEngine();
    }

    @Test
    @DisplayName("WHEN Group Buy reaches 90% THEN notify interested residents")
    void testGroupBuy90PercentMilestone() {
        var payload = Map.<String, Object>of(
                "dealTitle", "Premium Basmati Rice",
                "progressPercent", 92
        );

        var result = ruleEngine.evaluate(
                "Group Buy 90% Milestone",
                "progressPercent",
                AutomationRuleEngine.ComparisonOperator.GTE,
                90,
                "SEND_PUSH_NOTIFICATION",
                payload
        );

        assertTrue(result.isTriggered());
        assertEquals("SEND_PUSH_NOTIFICATION", result.triggeredAction());
    }

    @Test
    @DisplayName("WHEN maintenance becomes overdue THEN send reminder")
    void testMaintenanceOverdueReminder() {
        var payload = Map.<String, Object>of(
                "flatNumber", "Tower A - 302",
                "daysOverdue", 3
        );

        var result = ruleEngine.evaluate(
                "Overdue Maintenance Reminder",
                "daysOverdue",
                AutomationRuleEngine.ComparisonOperator.GTE,
                1,
                "SEND_WHATSAPP_REMINDER",
                payload
        );

        assertTrue(result.isTriggered());
    }

    @Test
    @DisplayName("WHEN visitor enters ➔ WAIT 3 hours ➔ IF still present THEN alert security")
    void testVisitorDelayedSecurityWorkflowTriggered() {
        var initialPayload = Map.<String, Object>of("entryGateRecorded", true);
        var postDelayState = Map.<String, Object>of("isStillInsideCampus", true);

        var result = ruleEngine.evaluateDelayedWorkflow(
                "Visitor 3-Hour Delayed Security Alert",
                "entryGateRecorded",
                AutomationRuleEngine.ComparisonOperator.EQUALS_BOOLEAN,
                true,
                "isStillInsideCampus",
                AutomationRuleEngine.ComparisonOperator.EQUALS_BOOLEAN,
                true,
                "NOTIFY_SECURITY_DESK",
                initialPayload,
                postDelayState
        );

        assertTrue(result.isTriggered());
        assertTrue(result.isDelayed());
        assertEquals("NOTIFY_SECURITY_DESK", result.triggeredAction());
    }

    @Test
    @DisplayName("WHEN visitor exits within 3 hours THEN cancel delayed security alert")
    void testVisitorDelayedSecurityWorkflowCancelledEarly() {
        var initialPayload = Map.<String, Object>of("entryGateRecorded", true);
        var postDelayState = Map.<String, Object>of("isStillInsideCampus", false); // Visitor exited!

        var result = ruleEngine.evaluateDelayedWorkflow(
                "Visitor 3-Hour Delayed Security Alert",
                "entryGateRecorded",
                AutomationRuleEngine.ComparisonOperator.EQUALS_BOOLEAN,
                true,
                "isStillInsideCampus",
                AutomationRuleEngine.ComparisonOperator.EQUALS_BOOLEAN,
                true,
                "NOTIFY_SECURITY_DESK",
                initialPayload,
                postDelayState
        );

        assertFalse(result.isTriggered());
        assertTrue(result.isDelayed());
        assertTrue(result.reason().contains("cancelled early"));
    }
}