package com.manacommunity.api.onboarding.cutover.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CutoverExecutionResultDto {

    public enum Status {
        CUTOVER_SUCCESSFUL,
        CUTOVER_ABORTED_PREWARM_MISSING,
        CUTOVER_ABORTED_HIGH_DUAL_WRITE_LAG
    }

    private Long communityId;
    private LocalDateTime cutoverTime;
    private Status status;
    private int activeGateControllersCount;
    private boolean peakMorningTrafficReady;
    private List<String> auditLog;
}
