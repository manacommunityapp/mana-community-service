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
public class CutoverReadinessAuditDto {

    private Long communityId;
    private LocalDateTime cutoverScheduledTime;
    private int totalGuardTablets;
    private int tabletsPrewarmedAndReady;
    private List<String> tabletsPendingPrewarm;
    private int rfidTagsSynced;
    private double rfidSyncCoveragePercentage;
    private long shadowDualWriteLagSeconds;
    private boolean readyFor6AmCutover;
    private List<String> blockingIssues;
}
