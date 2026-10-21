package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CposCrmAnalyticsDto {
    private long totalLeads;
    private int overallConversionPct;
    private double avgDaysTotal;
    private long hotLeads;
    private List<FunnelStageDto> funnel;
    private Map<String, Long> sourceBreakdown;
    private Map<String, Double> stageVelocity;
    private List<AgentStatDto> agentStats;

    @Data
    @Builder
    public static class FunnelStageDto {
        private String stage;
        private long count;
        private int pct;
    }

    @Data
    @Builder
    public static class AgentStatDto {
        private String agent;
        private long totalLeads;
        private long closed;
        private long inNegotiation;
        private int avgScore;
        private long pipelineLakhs;
    }
}
