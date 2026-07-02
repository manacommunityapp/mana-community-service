package com.manacommunity.api.dto;

import java.util.List;

public record SportsMetaResponse(
    Long id,
    String name,
    String icon,
    String iconUrl,
    Long communityId,
    List<String> formats,
    Boolean active
) {}
