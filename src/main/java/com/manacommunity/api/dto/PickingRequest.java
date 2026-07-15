package com.manacommunity.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PickingRequest(
    Long pickingTypeId,
    Long locationId,
    Long locationDestId,
    LocalDateTime scheduledDate,
    String origin,
    List<MoveLineRequest> moveLines
) {}
