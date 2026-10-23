package com.manacommunity.api.notification.orchestrator.engine;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.*;
import com.manacommunity.api.notification.orchestrator.event.DomainEventType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRule {
    private String ruleId;
    private DomainEventType eventType;
    private NotificationCategory category;
    private NotificationPriority defaultPriority;
    private DeliveryStrategy defaultStrategy;
    private List<NotificationChannel> defaultChannels;
    private String titleTemplate;
    private String bodyTemplate;
    private String defaultTargetType;
    private String actionUrlTemplate;
}
