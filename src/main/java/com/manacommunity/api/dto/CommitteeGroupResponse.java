package com.manacommunity.api.dto;

import java.time.LocalDateTime;

public record CommitteeGroupResponse(
    Long id,
    Long communityId,
    String name,
    String description,
    Integer displayOrder,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
