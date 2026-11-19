package com.manacommunity.api.search;

import java.util.List;
import java.util.Map;

public record SearchResultDto(
        String id,
        String module,
        String title,
        String subtitle,
        String description,
        String imageUrl,
        List<String> tags,
        String deepLinkType,
        String deepLinkId,
        double rank,
        Map<String, Object> metadata
) {}
