package com.manacommunity.api.sports.scheduler.engine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintValidationReport {
    @Builder.Default
    private boolean valid = true;
    @Builder.Default
    private int flatConflicts = 0;
    @Builder.Default
    private int towerConflicts = 0;
    @Builder.Default
    private int restPeriodViolations = 0;
    @Builder.Default
    private int courtOverlapViolations = 0;
    @Builder.Default
    private int seedPlacementScore = 100;
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    @Builder.Default
    private List<String> violations = new ArrayList<>();

    public void addViolation(String message) {
        this.valid = false;
        this.violations.add(message);
    }

    public void addWarning(String message) {
        this.warnings.add(message);
    }
}
