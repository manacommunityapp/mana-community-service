package com.manacommunity.api.dto;

public record TrendingResponse(
    Long id,
    String topic,
    String topicType,
    int postCount,
    int engagementCount,
    double score
) {}
