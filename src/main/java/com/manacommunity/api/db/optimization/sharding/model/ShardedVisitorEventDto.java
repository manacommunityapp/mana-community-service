package com.manacommunity.api.db.optimization.sharding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShardedVisitorEventDto {

    private String eventId;
    private Long communityId;
    private int shardId;
    private String partitionKey; // e.g. COMMUNITY_1042#shard_04
    private String sortKey;      // e.g. 2026-11-27T18:30:00#EVT-1001
    private LocalDateTime timestamp;
    private String visitorName;
    private String flatNumber;
    private String entryGate;
}
