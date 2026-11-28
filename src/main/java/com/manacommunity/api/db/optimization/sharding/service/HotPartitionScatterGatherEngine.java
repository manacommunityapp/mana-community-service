package com.manacommunity.api.db.optimization.sharding.service;

import com.manacommunity.api.db.optimization.sharding.model.ShardedVisitorEventDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface HotPartitionScatterGatherEngine {

    int DEFAULT_SHARD_COUNT = 16;

    ShardedVisitorEventDto writeEvent(
            Long communityId,
            String visitorName,
            String flatNumber,
            String entryGate,
            LocalDateTime timestamp);

    List<ShardedVisitorEventDto> queryRangeSorted(
            Long communityId,
            LocalDateTime startTime,
            LocalDateTime endTime);

    Map<Integer, Long> getShardDistribution(Long communityId);

    void clear();
}
