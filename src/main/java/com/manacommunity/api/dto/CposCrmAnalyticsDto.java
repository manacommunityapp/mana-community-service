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
    private List<LeaderboardEntryDto> leaderboard;
    private List<CallOutcomeDto> callOutcomes;
    private List<HourlyActivityDto> hourlyActivity;
    private ForecastDto forecast;

    @Data
    @Builder
    public static class CallOutcomeDto {
        private String outcome;
        private long count;
    }

    @Data
    @Builder
    public static class HourlyActivityDto {
        private int hour;
        private long count;
    }

    @Data
    @Builder
    public static class ForecastDto {
        private long weightedRevenueLakhs;
        private long totalPipelineLeads;
        private List<StageForecastDto> byStage;
    }

    @Data
    @Builder
    public static class StageForecastDto {
        private String stage;
        private long leads;
        private int probabilityPct;
        private long weightedRevenueLakhs;
    }

    @Data
    @Builder
    public static class LeaderboardEntryDto {
        private int rank;
        private String agent;
        private long closedLeads;
        private long revenueLakhs;
        private long totalLeads;
        private int score;
    }

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
