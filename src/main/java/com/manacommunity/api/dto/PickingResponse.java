package com.manacommunity.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PickingResponse(
    Long id,
    String name,
    String state,
    Long partnerId,
    Long pickingTypeId,
    Long locationId,
    Long locationDestId,
    LocalDateTime scheduledDate,
    String origin,
    List<MoveLineResponse> moveLines
) {}
