package com.manacommunity.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SyncPullResponse(
    LocalDateTime lastSyncTimestamp,
    Map<String, List<Map<String, Object>>> updates
) {}
