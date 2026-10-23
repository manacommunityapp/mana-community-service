package com.manacommunity.api.automation.unit;

import com.manacommunity.api.automation.engine.ProjectLifecycleEngine;
import com.manacommunity.api.automation.engine.ProjectLifecycleEngine.ProjectStage;
import org.junit.jupiter.api.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProjectLifecycleEngine Unit Tests")
class ProjectLifecycleEngineTest {

    private ProjectLifecycleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ProjectLifecycleEngine();
    }

    @Test
    @DisplayName("Stage advance: APPROVED → PLANNING → PROCUREMENT → IN_PROGRESS")
    void testStageTransitions() {
        assertEquals(ProjectStage.PLANNING, engine.advanceLifecycleStage(ProjectStage.APPROVED));
        assertEquals(ProjectStage.PROCUREMENT, engine.advanceLifecycleStage(ProjectStage.PLANNING));
        assertEquals(ProjectStage.IN_PROGRESS, engine.advanceLifecycleStage(ProjectStage.PROCUREMENT));
        assertEquals(ProjectStage.QUALITY_CHECK, engine.advanceLifecycleStage(ProjectStage.IN_PROGRESS));
        assertEquals(ProjectStage.COMPLETED, engine.advanceLifecycleStage(ProjectStage.QUALITY_CHECK));
    }

    @Test
    @DisplayName("Stage advance: COMPLETED stage stays COMPLETED")
    void testAdvanceBeyondCompleted() {
        assertEquals(ProjectStage.COMPLETED, engine.advanceLifecycleStage(ProjectStage.COMPLETED));
    }

    @Test
    @DisplayName("Budget validation: within budget passes, over budget fails")
    void testBudgetValidation() {
        // Lift Replacement: 10.5L spent, adding 4L expense, budget 18L — OK
        assertTrue(engine.validateBudgetApproval(400_000, 1_050_000, 1_800_000));
        // Over budget: spent 17L, trying to add 5L, budget 18L — FAIL
        assertFalse(engine.validateBudgetApproval(500_000, 1_700_000, 1_800_000));
        // Exact budget limit — OK
        assertTrue(engine.validateBudgetApproval(0, 1_800_000, 1_800_000));
    }

    @Test
    @DisplayName("Completion percentage: average of milestone progress values")
    void testCompletionPercentage() {
        // Lift: 100%, 100%, 100%, 65%, 0%, 0% → avg = 60.83 → rounded 61
        int result = engine.computeCompletionPercentage(List.of(100, 100, 100, 65, 0, 0));
        assertEquals(61, result);
        // No milestones → 0
        assertEquals(0, engine.computeCompletionPercentage(List.of()));
        // All complete → 100
        assertEquals(100, engine.computeCompletionPercentage(List.of(100, 100, 100)));
    }

    @Test
    @DisplayName("Procurement gate: requires budget approval + at least 1 vendor quote")
    void testProcurementGate() {
        assertTrue(engine.canAdvanceToProcurement(true, 3));
        assertFalse(engine.canAdvanceToProcurement(false, 3));  // no budget
        assertFalse(engine.canAdvanceToProcurement(true, 0));   // no quotes
    }

    @Test
    @DisplayName("Completion gate: all milestones 100% + QC signed off")
    void testCompletionGate() {
        assertTrue(engine.canMarkCompleted(List.of(100, 100, 100), true));
        assertFalse(engine.canMarkCompleted(List.of(100, 100, 65), true));   // incomplete milestone
        assertFalse(engine.canMarkCompleted(List.of(100, 100, 100), false)); // QC not signed
    }
}