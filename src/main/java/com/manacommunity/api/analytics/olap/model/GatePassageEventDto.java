package com.manacommunity.api.analytics.olap.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * High-velocity gate passage event streamed via Kafka topic: gate.passage.events.v1
 * Partitioned by community_id for ClickHouse ingestion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatePassageEventDto {

    private UUID eventId;
    private Long communityId;
    private String gateId;
    private Long guardId;
    private String visitorType; // DELIVERY, CAB, GUEST, SERVICE, DAILY_HELP
    private String entryStatus; // APPROVED, REJECTED, AUTO_APPROVED, OVERDUE
    private LocalDateTime requestTimestamp;
    private LocalDateTime approvalTimestamp;
    private LocalDateTime gatePassageTimestamp;
    private double approvalDurationSeconds;
    private boolean isPreAuthorized;
}
