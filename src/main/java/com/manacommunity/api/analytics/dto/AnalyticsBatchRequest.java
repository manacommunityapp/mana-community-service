package com.manacommunity.api.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsBatchRequest {

    private List<AnalyticsEventItemDto> events;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsEventItemDto {
        private String event;
        private Map<String, Object> props;
        private String ts;
        private Long communityId;
        private String userHash;
        private String cohort;
        private String sessionId;
    }
}
