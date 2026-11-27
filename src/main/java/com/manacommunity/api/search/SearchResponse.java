package com.manacommunity.api.search;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        List<SearchResultDto> results,
        Map<String, Long> facets,
        long total,
        int page,
        int size
) {}
