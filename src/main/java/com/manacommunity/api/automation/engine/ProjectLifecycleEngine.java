package com.manacommunity.api.automation.engine;

import java.util.List;
import java.util.Map;

/**
 * ProjectLifecycleEngine — manages community project stage transitions,
 * budget validation, and milestone-based completion calculation.
 *
 * Lifecycle: PROPOSAL → VOTING → APPROVED → PLANNING → PROCUREMENT
 *            → IN_PROGRESS → QUALITY_CHECK → COMPLETED
 */
public class ProjectLifecycleEngine {

    public enum ProjectStage {
        PROPOSAL, VOTING, APPROVED, PLANNING, PROCUREMENT,
        IN_PROGRESS, QUALITY_CHECK, COMPLETED
    }

    private static final List<ProjectStage> STAGE_ORDER = List.of(
        ProjectStage.PROPOSAL,
        ProjectStage.VOTING,
        ProjectStage.APPROVED,
        ProjectStage.PLANNING,
        ProjectStage.PROCUREMENT,
        ProjectStage.IN_PROGRESS,
        ProjectStage.QUALITY_CHECK,
        ProjectStage.COMPLETED
    );

    /**
     * Advance the project to the next lifecycle stage.
     *
     * @param currentStage the project's current stage
     * @return the next stage, or COMPLETED if already at end
     * @throws IllegalArgumentException if currentStage is not in the lifecycle
     */
    public ProjectStage advanceLifecycleStage(ProjectStage currentStage) {
        int idx = STAGE_ORDER.indexOf(currentStage);
        if (idx < 0) {
            throw new IllegalArgumentException("Unknown project stage: " + currentStage);
        }
        if (idx >= STAGE_ORDER.size() - 1) {
            return ProjectStage.COMPLETED;
        }
        return STAGE_ORDER.get(idx + 1);
    }

    /**
     * Validate that a requested expense does not exceed the approved project budget.
     *
     * @param requestedAmount  the new expense amount to add
     * @param alreadySpent     total already spent on the project
     * @param approvedBudget   total approved budget (including contingency)
     * @return true if the expense is within budget, false if it would cause an overrun
     */
    public boolean validateBudgetApproval(double requestedAmount, double alreadySpent, double approvedBudget) {
        return (alreadySpent + requestedAmount) <= approvedBudget;
    }

    /**
     * Compute overall project completion as the average of all milestone progress values.
     *
     * @param milestoneProgress a list of per-milestone progress percentages (0–100)
     * @return average completion percentage, or 0 if no milestones provided
     */
    public int computeCompletionPercentage(List<Integer> milestoneProgress) {
        if (milestoneProgress == null || milestoneProgress.isEmpty()) return 0;
        int sum = milestoneProgress.stream().mapToInt(Integer::intValue).sum();
        return Math.round((float) sum / milestoneProgress.size());
    }

    /**
     * Check whether the project can be advanced to PROCUREMENT stage.
     * Requires: budget approved and at least one vendor quote attached.
     */
    public boolean canAdvanceToProcurement(boolean budgetApproved, int vendorQuotesCount) {
        return budgetApproved && vendorQuotesCount >= 1;
    }

    /**
     * Check whether the project can be marked COMPLETED.
     * Requires: all milestones completed and quality check signed off.
     */
    public boolean canMarkCompleted(List<Integer> milestoneProgress, boolean qualityCheckSignedOff) {
        if (!qualityCheckSignedOff) return false;
        return milestoneProgress.stream().allMatch(p -> p == 100);
    }
}