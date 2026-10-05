package com.manacommunity.api.notification.orchestrator.engine;

import com.manacommunity.api.notification.orchestrator.NotificationEnums.NotificationPriority;
import com.manacommunity.api.notification.orchestrator.event.DomainEvent;
import com.manacommunity.api.notification.orchestrator.event.DomainEventType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationPriorityResolver {

    public NotificationPriority resolvePriority(DomainEvent event, NotificationRule rule) {
        if (event.getEventType() == DomainEventType.EMERGENCY_BROADCAST ||
            event.getEventType() == DomainEventType.SECURITY_ALERT) {
            return NotificationPriority.CRITICAL;
        }

        Map<String, Object> payload = event.getPayload();
        if (payload != null) {
            if (payload.containsKey("isCritical") && Boolean.TRUE.equals(payload.get("isCritical"))) {
                return NotificationPriority.CRITICAL;
            }
            if (payload.containsKey("amount")) {
                try {
                    double amt = Double.parseDouble(String.valueOf(payload.get("amount")));
                    if (amt > 25000) return NotificationPriority.HIGH;
                } catch (Exception ignored) {}
            }
        }

        return rule != null && rule.getDefaultPriority() != null ? rule.getDefaultPriority() : NotificationPriority.NORMAL;
    }
}
