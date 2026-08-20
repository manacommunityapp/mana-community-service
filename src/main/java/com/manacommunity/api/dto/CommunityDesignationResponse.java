package com.manacommunity.api.dto;

public record CommunityDesignationResponse(
        Long id,
        String name,
        Long communityId,
        Integer displayOrder,
        Boolean isDefault
) {}
