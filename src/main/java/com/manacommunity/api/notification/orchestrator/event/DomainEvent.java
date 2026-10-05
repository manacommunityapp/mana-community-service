package com.manacommunity.api.notification.orchestrator.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {
    private String eventId;
    private DomainEventType eventType;
    private String aggregateType;
    private String aggregateId;
    private Long communityId;
    private Long actorId;
    
    // Target routing
    @Builder.Default
    private String targetType = "DIRECT_USER"; // DIRECT_USER, COMMUNITY, TOWER, ROLE, FLAT
    private String targetId;
    private Long directRecipientUserId;
    
    private Map<String, Object> payload;
    
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
