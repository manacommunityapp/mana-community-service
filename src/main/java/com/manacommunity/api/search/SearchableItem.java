package com.manacommunity.api.search;

import java.util.List;
import java.util.Map;

public record SearchableItem(
        Long communityId,
        SearchModule module,
        String entityId,
        String title,
        String subtitle,
        String description,
        List<String> tags,
        String imageUrl,
        String deepLinkType,
        String deepLinkId,
        SearchVisibility visibility,
        Long ownerUserId,
        Map<String, Object> metadata
) {}
